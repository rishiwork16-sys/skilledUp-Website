package com.skilledup.course.service;

import com.skilledup.course.entity.Course;
import com.skilledup.course.entity.Module;
import com.skilledup.course.entity.Video;
import com.skilledup.course.repository.CourseRepository;
import com.skilledup.course.repository.ModuleRepository;
import com.skilledup.course.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private CloudFrontService cloudFrontService;

    public Course createCourse(Course course) {
        // Simple slug generation
        if (course.getSlug() == null || course.getSlug().isEmpty()) {
            course.setSlug(course.getTitle().toLowerCase().replace(" ", "-"));
        }

        if (course.getModules() != null) {
            course.getModules().forEach(module -> module.setCourse(course));
        }

        if (course.getKeyHighlights() != null) {
            course.getKeyHighlights().forEach(kh -> kh.setCourse(course));
        }
        
        if (course.getCareerOpportunities() != null) {
            course.getCareerOpportunities().forEach(co -> co.setCourse(course));
        }

        return courseRepository.save(course);
    }

    public List<Course> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        courses.forEach(this::resolveThumbnailUrl);
        return courses;
    }

    public Course getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        resolveThumbnailUrl(course);
        return course;
    }

    public Course getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        resolveThumbnailUrl(course);
        return course;
    }

    private Course findCourseEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    private void resolveThumbnailUrl(Course course) {
        if (course.getThumbnailUrl() != null && !course.getThumbnailUrl().startsWith("http")) {
            course.setThumbnailUrl(s3Service.generatePresignedUrl(course.getThumbnailUrl()));
        }

        if (course.getCertificateUrl() != null && !course.getCertificateUrl().startsWith("http")) {
            course.setCertificateUrl(s3Service.generatePresignedUrl(course.getCertificateUrl()));
        }

        if (course.getIntroVideoUrl() != null && !course.getIntroVideoUrl().startsWith("http")) {
            course.setIntroVideoUrl(s3Service.generatePresignedUrl(course.getIntroVideoUrl()));
        }

        if (course.getMentors() != null && !course.getMentors().isEmpty()) {
            for (var mentor : course.getMentors()) {
                if (mentor == null) continue;
                String key = mentor.getImageKey();
                if (key == null || key.isBlank()) continue;
                if (key.startsWith("http")) {
                    mentor.setImageUrl(key);
                } else {
                    mentor.setImageUrl(s3Service.generatePresignedUrl(key));
                }
            }
        }

        if (course.getToolsCovered() != null && !course.getToolsCovered().isEmpty()) {
            java.util.List<String> resolved = new java.util.ArrayList<>(course.getToolsCovered().size());
            for (String key : course.getToolsCovered()) {
                if (key == null || key.isBlank()) continue;
                if (key.startsWith("http")) {
                    resolved.add(key);
                } else {
                    resolved.add(s3Service.generatePresignedUrl(key));
                }
            }
            course.setToolsCovered(resolved);
        }
    }

    @Transactional
    public Course uploadThumbnail(Long courseId, MultipartFile file) throws IOException {
        Course course = findCourseEntity(courseId); // Use clean fetch

        String s3Key = s3Service.uploadFile(file);
        course.setThumbnailUrl(s3Key);
        return courseRepository.save(course);
    }

    @Transactional
    public Course uploadCertificate(Long courseId, MultipartFile file) throws IOException {
        Course course = findCourseEntity(courseId);
        String s3Key = s3Service.uploadFile(file);
        course.setCertificateUrl(s3Key);
        return courseRepository.save(course);
    }

    @Transactional
    public Course uploadIntroVideo(Long courseId, MultipartFile file) throws IOException {
        Course course = findCourseEntity(courseId);
        String s3Key = s3Service.uploadFile(file);
        course.setIntroVideoUrl(s3Key);
        return courseRepository.save(course);
    }

    @Transactional
    public String uploadMentorImage(Long courseId, MultipartFile file) throws IOException {
        findCourseEntity(courseId);
        return s3Service.uploadFile(file);
    }

    public String generatePresignedUrl(String key) {
        return s3Service.generatePresignedUrl(key);
    }

    @Transactional
    public Course uploadToolsCovered(Long courseId, java.util.List<MultipartFile> files, boolean append) throws IOException {
        Course course = findCourseEntity(courseId);
        if (course.getToolsCovered() == null) {
            course.setToolsCovered(new java.util.ArrayList<>());
        }
        if (!append) {
            course.getToolsCovered().clear();
        }
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String s3Key = s3Service.uploadFile(file);
                course.getToolsCovered().add(s3Key);
            }
        }
        return courseRepository.save(course);
    }

    @Transactional
    public Course deleteToolCoveredAtIndex(Long courseId, int index) {
        Course course = findCourseEntity(courseId);
        if (course.getToolsCovered() == null || index < 0 || index >= course.getToolsCovered().size()) {
            throw new IllegalArgumentException("Invalid tool index");
        }
        course.getToolsCovered().remove(index);
        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, Course courseDetails) {
        try {
            Course course = findCourseEntity(id);

            course.setTitle(courseDetails.getTitle());
            if (courseDetails.getTitle() != null) {
                course.setSlug(courseDetails.getTitle().toLowerCase().replace(" ", "-"));
            }
            course.setDescription(courseDetails.getDescription());
            course.setPrice(courseDetails.getPrice());
            course.setOriginalPrice(courseDetails.getOriginalPrice());
            course.setDiscount(courseDetails.getDiscount());
            course.setDuration(courseDetails.getDuration());
            course.setMode(courseDetails.getMode());
            course.setCategory(courseDetails.getCategory());
            
            if (courseDetails.getThumbnailUrl() != null
                    && !courseDetails.getThumbnailUrl().isBlank()
                    && !courseDetails.getThumbnailUrl().startsWith("http")) {
                course.setThumbnailUrl(courseDetails.getThumbnailUrl());
            }

            if (courseDetails.getCertificateUrl() != null
                    && !courseDetails.getCertificateUrl().isBlank()
                    && !courseDetails.getCertificateUrl().startsWith("http")) {
                course.setCertificateUrl(courseDetails.getCertificateUrl());
            }

            if (courseDetails.getIntroVideoUrl() != null
                    && !courseDetails.getIntroVideoUrl().isBlank()
                    && !courseDetails.getIntroVideoUrl().startsWith("http")) {
                course.setIntroVideoUrl(courseDetails.getIntroVideoUrl());
            }

            if (courseDetails.getMentors() != null) {
                if (course.getMentors() == null) {
                    course.setMentors(new java.util.ArrayList<>());
                } else {
                    course.getMentors().clear();
                }
                course.getMentors().addAll(courseDetails.getMentors());
            }

            if (courseDetails.getKeyHighlights() != null) {
                if (course.getKeyHighlights() == null) {
                    course.setKeyHighlights(new java.util.ArrayList<>());
                } else {
                    course.getKeyHighlights().clear();
                }
                // Reset IDs to avoid Hibernate conflict
                courseDetails.getKeyHighlights().forEach(kh -> kh.setId(null));
                
                course.getKeyHighlights().addAll(courseDetails.getKeyHighlights());
                course.getKeyHighlights().forEach(kh -> kh.setCourse(course));
            }

            if (courseDetails.getCareerOpportunities() != null) {
                if (course.getCareerOpportunities() == null) {
                    course.setCareerOpportunities(new java.util.ArrayList<>());
                } else {
                    course.getCareerOpportunities().clear();
                }
                courseDetails.getCareerOpportunities().forEach(co -> co.setId(null));
                
                course.getCareerOpportunities().addAll(courseDetails.getCareerOpportunities());
                course.getCareerOpportunities().forEach(co -> co.setCourse(course));
            }

            if (courseDetails.getSkills() != null) {
                if (course.getSkills() == null) {
                    course.setSkills(new java.util.ArrayList<>());
                } else {
                    course.getSkills().clear();
                }
                course.getSkills().addAll(courseDetails.getSkills());
            }

            if (courseDetails.getFaqs() != null) {
                if (course.getFaqs() == null) {
                    course.setFaqs(new java.util.ArrayList<>());
                } else {
                    course.getFaqs().clear();
                }
                course.getFaqs().addAll(courseDetails.getFaqs());
            }

            return courseRepository.save(course);
        } catch (Exception e) {
            System.err.println("Error updating course: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update course: " + e.getMessage());
        }
    }

    @Transactional
    public Module addModule(Long courseId, Module module) {
        Course course = findCourseEntity(courseId); // Fix: Use raw entity
        module.setCourse(course);
        return moduleRepository.save(module);
    }

    @Transactional
    public Video addVideo(Long moduleId, String title, String description, MultipartFile file, String duration)
            throws IOException {
        try {
            System.out.println("Uploading video for module ID: " + moduleId);
            Module module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));

            System.out.println("Found module: " + module.getId());
            System.out.println("Starting HLS processing for file: " + file.getOriginalFilename());

            // Use VideoProcessingService to transcode and upload HLS
            String s3Key = videoProcessingService.processAndUploadVideo(file);
            System.out.println("HLS upload successful, master playlist key: " + s3Key);

            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setS3Key(s3Key);
            video.setDuration(duration);
            video.setModule(module);
            video.setIsLocked(true); // Default locked

            Video savedVideo = videoRepository.save(video);
            System.out.println("Video saved to DB: " + savedVideo.getId());
            return savedVideo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Video processing interrupted", e);
        } catch (Exception e) {
            System.err.println("ERROR in addVideo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e); // Rethrow to let controller handle it
        }
    }

    public String getVideoUrl(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        if (!cloudFrontService.isConfigured()) {
            return "/api/courses/hls/" + videoId + "/master.m3u8";
        }
        return cloudFrontService.generateSignedUrl(video.getS3Key());
    }

    public String getVideoS3BaseKey(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        String s3Key = video.getS3Key();
        int lastSlash = s3Key.lastIndexOf('/');
        if (lastSlash <= 0) {
            return s3Key;
        }
        return s3Key.substring(0, lastSlash);
    }

    public Long getCourseIdByVideoId(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return video.getModule().getCourse().getId();
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}

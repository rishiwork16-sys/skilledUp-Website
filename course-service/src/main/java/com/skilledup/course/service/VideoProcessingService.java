package com.skilledup.course.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import java.nio.file.DirectoryStream;

@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final S3Service s3Service;

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    private String resolvedFfmpegPath;
    private String resolvedFfprobePath;

    @PostConstruct
    private void initializeExecutables() {
        this.resolvedFfmpegPath = resolveExecutablePath(ffmpegPath, "ffmpeg");
        this.resolvedFfprobePath = resolveExecutablePath(deriveSiblingExecutable(resolvedFfmpegPath, "ffprobe"), "ffprobe");
    }

    public String processAndUploadVideo(MultipartFile file) throws IOException, InterruptedException {
        // 1. Save to local temp
        String videoId = UUID.randomUUID().toString();
        Path tempDir = Files.createTempDirectory("video_processing_" + videoId);
        File sourceFile = tempDir.resolve("input.mp4").toFile();
        file.transferTo(sourceFile);

        // 2. Transcode
        Path hlsOutputDir = tempDir.resolve("hls");
        Files.createDirectories(hlsOutputDir);
        
        // Create subdirectories for streams
        for (int i = 0; i < 4; i++) {
            Files.createDirectories(hlsOutputDir.resolve("stream_" + i));
        }

        transcodeToHls(sourceFile, hlsOutputDir);

        // 3. Upload to S3
        String s3BaseKey = "videos/" + videoId;
        uploadDirectory(hlsOutputDir.toFile(), s3BaseKey);

        // 4. Cleanup
        try {
            deleteDirectory(tempDir);
        } catch (IOException e) {
            System.err.println("Failed to cleanup temp directory: " + e.getMessage());
        }

        return s3BaseKey + "/master.m3u8";
    }

    private void transcodeToHls(File sourceFile, Path outputDir) throws IOException, InterruptedException {
        String inputPath = normalizeForFfmpeg(sourceFile.toPath());
        String outputDirNormalized = normalizeForFfmpeg(outputDir);

        boolean hasAudio = hasAudioStream(sourceFile.toPath());

        ProcessBuilder pb;
        if (hasAudio) {
            pb = new ProcessBuilder(
                resolvedFfmpegPath, "-i", inputPath,
                "-filter_complex", "[0:v]split=4[v1][v2][v3][v4];[v1]scale=w=1280:h=720[v1out];[v2]scale=w=854:h=480[v2out];[v3]scale=w=640:h=360[v3out];[v4]scale=w=426:h=240[v4out]",
                "-map", "[v1out]", "-c:v:0", "libx264", "-b:v:0", "2500k", "-maxrate:v:0", "2675k", "-bufsize:v:0", "3750k",
                "-map", "[v2out]", "-c:v:1", "libx264", "-b:v:1", "1400k", "-maxrate:v:1", "1500k", "-bufsize:v:1", "2100k",
                "-map", "[v3out]", "-c:v:2", "libx264", "-b:v:2", "800k", "-maxrate:v:2", "856k", "-bufsize:v:2", "1200k",
                "-map", "[v4out]", "-c:v:3", "libx264", "-b:v:3", "400k", "-maxrate:v:3", "428k", "-bufsize:v:3", "600k",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-f", "hls",
                "-hls_time", "10",
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", outputDirNormalized + "/stream_%v/data%03d.ts",
                "-master_pl_name", "master.m3u8",
                "-var_stream_map", "v:0,a:0 v:1,a:0 v:2,a:0 v:3,a:0",
                outputDirNormalized + "/stream_%v/playlist.m3u8"
            );
        } else {
            pb = new ProcessBuilder(
                resolvedFfmpegPath, "-i", inputPath,
                "-filter_complex", "[0:v]split=4[v1][v2][v3][v4];[v1]scale=w=1280:h=720[v1out];[v2]scale=w=854:h=480[v2out];[v3]scale=w=640:h=360[v3out];[v4]scale=w=426:h=240[v4out]",
                "-map", "[v1out]", "-c:v:0", "libx264", "-b:v:0", "2500k", "-maxrate:v:0", "2675k", "-bufsize:v:0", "3750k",
                "-map", "[v2out]", "-c:v:1", "libx264", "-b:v:1", "1400k", "-maxrate:v:1", "1500k", "-bufsize:v:1", "2100k",
                "-map", "[v3out]", "-c:v:2", "libx264", "-b:v:2", "800k", "-maxrate:v:2", "856k", "-bufsize:v:2", "1200k",
                "-map", "[v4out]", "-c:v:3", "libx264", "-b:v:3", "400k", "-maxrate:v:3", "428k", "-bufsize:v:3", "600k",
                "-f", "hls",
                "-hls_time", "10",
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", outputDirNormalized + "/stream_%v/data%03d.ts",
                "-master_pl_name", "master.m3u8",
                "-var_stream_map", "v:0 v:1 v:2 v:3",
                outputDirNormalized + "/stream_%v/playlist.m3u8"
            );
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();
        CapturedProcessOutput captured = captureProcessOutput(process, 32768);
        int exitCode = process.waitFor();
        captured.await();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg transcoding failed with exit code " + exitCode + ". Output: " + captured.output);
        }
    }

    private void uploadDirectory(File dir, String s3BaseKey) {
        if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            if (entries == null) {
                return;
            }
            for (File file : entries) {
                if (file.isDirectory()) {
                    uploadDirectory(file, s3BaseKey + "/" + file.getName());
                } else {
                    s3Service.uploadFile(file, s3BaseKey + "/" + file.getName());
                }
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    private boolean hasAudioStream(Path inputFile) {
        if (resolvedFfprobePath != null && !resolvedFfprobePath.isBlank()) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    resolvedFfprobePath,
                    "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=codec_type",
                    "-of", "csv=p=0",
                    normalizeForFfmpeg(inputFile)
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                CapturedProcessOutput captured = captureProcessOutput(process, 8192);
                int exitCode = process.waitFor();
                captured.await();
                if (exitCode == 0) {
                    return captured.output != null && !captured.output.trim().isEmpty();
                }
            } catch (Exception ignored) {
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(resolvedFfmpegPath, "-i", normalizeForFfmpeg(inputFile));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            CapturedProcessOutput captured = captureProcessOutput(process, 16384);
            process.waitFor();
            captured.await();
            return captured.output != null && captured.output.contains("Audio:");
        } catch (Exception e) {
            return true;
        }
    }

    private String normalizeForFfmpeg(Path path) {
        String raw = path.toAbsolutePath().toString();
        if (File.separatorChar == '\\') {
            return raw.replace('\\', '/');
        }
        return raw;
    }

    private String resolveExecutablePath(String configuredValue, String executableName) {
        String effective = (configuredValue == null || configuredValue.isBlank()) ? executableName : configuredValue.trim();

        try {
            Path asPath = Paths.get(effective);
            if (Files.isRegularFile(asPath)) {
                return asPath.toAbsolutePath().toString();
            }
        } catch (Exception ignored) {
        }

        Optional<Path> bundled = findBundledExecutable(Paths.get(System.getProperty("user.dir")), executableName);
        if (bundled.isPresent()) {
            return bundled.get().toAbsolutePath().toString();
        }

        return effective;
    }

    private Optional<Path> findBundledExecutable(Path startDir, String executableName) {
        Path current = startDir;
        String resolvedName = (File.separatorChar == '\\') ? executableName + ".exe" : executableName;
        for (int depth = 0; depth < 10 && current != null; depth++) {
            Path ffmpegRoot = current.resolve("ffmpeg");
            if (Files.isDirectory(ffmpegRoot)) {
                Path direct = ffmpegRoot.resolve("bin").resolve(resolvedName);
                if (Files.isRegularFile(direct)) {
                    return Optional.of(direct);
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(ffmpegRoot)) {
                    for (Path child : stream) {
                        Path candidate = child.resolve("bin").resolve(resolvedName);
                        if (Files.isRegularFile(candidate)) {
                            return Optional.of(candidate);
                        }
                    }
                } catch (IOException ignored) {
                }
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private String deriveSiblingExecutable(String baseExecutable, String siblingName) {
        if (baseExecutable == null || baseExecutable.isBlank()) {
            return siblingName;
        }
        try {
            Path basePath = Paths.get(baseExecutable);
            Path parent = basePath.getParent();
            if (parent == null) {
                return siblingName;
            }
            String resolvedName = (File.separatorChar == '\\') ? siblingName + ".exe" : siblingName;
            return parent.resolve(resolvedName).toString();
        } catch (Exception ignored) {
            return siblingName;
        }
    }

    private CapturedProcessOutput captureProcessOutput(Process process, int maxBytes) {
        CapturedProcessOutput captured = new CapturedProcessOutput();
        Thread reader = new Thread(() -> captured.output = readUpTo(process.getInputStream(), maxBytes));
        reader.setDaemon(true);
        reader.start();
        captured.reader = reader;
        return captured;
    }

    private String readUpTo(InputStream inputStream, int maxBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                if (total < maxBytes) {
                    int remaining = maxBytes - total;
                    int toWrite = Math.min(remaining, read);
                    out.write(buffer, 0, toWrite);
                }
                total += read;
                if (total >= maxBytes) {
                    break;
                }
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static class CapturedProcessOutput {
        private Thread reader;
        private String output;

        private void await() {
            if (reader == null) {
                return;
            }
            try {
                reader.join(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

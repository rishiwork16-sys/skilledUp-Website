import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import DashboardLayout from '../../components/DashboardLayout';
import { ArrowLeft, Save, Upload, X, Image as ImageIcon, Loader2 } from 'lucide-react';
import api from '../../services/api';
import courseCategoryService from '../../services/courseCategory.service';

const CourseEdit = () => {
    const { courseId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [categories, setCategories] = useState([]);
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        subtitle: '',
        price: '',
        originalPrice: '',
        discount: 0,
        duration: '',
        mode: 'Online',
        category: '',
        thumbnailUrl: '',
        certificateUrl: '',
        introVideoUrl: ''
    });
    const [thumbnail, setThumbnail] = useState(null);
    const [thumbnailPreviewUrl, setThumbnailPreviewUrl] = useState(null);
    const [introVideo, setIntroVideo] = useState(null);
    const [introVideoPreviewUrl, setIntroVideoPreviewUrl] = useState(null);
    const [certificate, setCertificate] = useState(null);
    const [certificatePreviewUrl, setCertificatePreviewUrl] = useState(null);
    const [brochure, setBrochure] = useState(null);
    const [brochureUrl, setBrochureUrl] = useState(null);
    const [toolsCoveredFiles, setToolsCoveredFiles] = useState([]);
    const [toolsCoveredPreviews, setToolsCoveredPreviews] = useState([]);
    const [existingToolsCovered, setExistingToolsCovered] = useState([]);
    const [mentors, setMentors] = useState([
        {
            name: "",
            role: "",
            workExpYears: "",
            teachExpYears: "",
            about: "",
            imageKey: "",
            imageUrl: "",
            imageFile: null,
            imagePreviewUrl: ""
        }
    ]);
    const [keyHighlights, setKeyHighlights] = useState([]);
    const [skills, setSkills] = useState([""]);
    const [careerOpportunities, setCareerOpportunities] = useState([
        { title: "", description: "" }
    ]);
    const [faqs, setFaqs] = useState([{ question: "", answer: "" }]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [courseResponse, categoriesResponse] = await Promise.all([
                    api.get(`/admin/courses/${courseId}`),
                    courseCategoryService.getAllCategories()
                ]);

                setCategories(categoriesResponse);

                const data = courseResponse.data;
                setFormData({
                    title: data.title || '',
                    description: data.description || '',
                    subtitle: data.subtitle || '100% Job Guarantee (T&C Apply)',
                    price: data.price ? data.price.toString() : '',
                    originalPrice: data.originalPrice ? data.originalPrice.toString() : '',
                    discount: data.discount || 0,
                    duration: data.duration || '',
                    mode: data.mode || 'Online',
                    category: data.category || (categoriesResponse.length > 0 ? categoriesResponse[0].name : ''),
                    thumbnailUrl: data.thumbnailUrl || '',
                    certificateUrl: data.certificateUrl || '',
                    introVideoUrl: data.introVideoUrl || ''
                });
                setThumbnailPreviewUrl(data.thumbnailUrl || null);
                setIntroVideoPreviewUrl(data.introVideoUrl || null);
                setCertificatePreviewUrl(data.certificateUrl || null);
                setBrochureUrl(data.brochureUrl || null);

                if (Array.isArray(data.toolsCovered) && data.toolsCovered.length > 0) {
                    setExistingToolsCovered(data.toolsCovered);
                } else {
                    setExistingToolsCovered([]);
                }

                if (data.keyHighlights && data.keyHighlights.length > 0) {
                    setKeyHighlights(data.keyHighlights);
                } else {
                    setKeyHighlights([
                        { title: "Product (What You Learn)", items: [""] },
                        { title: "Process (How You Learn)", items: [""] },
                        { title: "Placement (Career Outcomes)", items: [""] }
                    ]);
                }

                if (data.skills && data.skills.length > 0) {
                    setSkills(data.skills);
                }

                if (data.careerOpportunities && data.careerOpportunities.length > 0) {
                    setCareerOpportunities(data.careerOpportunities);
                } else {
                    setCareerOpportunities([{ title: "", description: "" }]);
                }

                if (data.faqs && data.faqs.length > 0) {
                    setFaqs(data.faqs);
                } else {
                    setFaqs([{ question: "", answer: "" }]);
                }

                if (Array.isArray(data.mentors) && data.mentors.length > 0) {
                    setMentors(
                        data.mentors.map(m => ({
                            name: m.name || "",
                            role: m.role || "",
                            workExpYears: m.workExpYears ?? "",
                            teachExpYears: m.teachExpYears ?? "",
                            about: m.about || "",
                            imageKey: m.imageKey || "",
                            imageUrl: m.imageUrl || "",
                            imageFile: null,
                            imagePreviewUrl: m.imageUrl || ""
                        }))
                    );
                } else {
                    setMentors([{ name: "", role: "", workExpYears: "", teachExpYears: "", about: "", imageKey: "", imageUrl: "", imageFile: null, imagePreviewUrl: "" }]);
                }
            } catch (error) {
                console.error("Failed to fetch data", error);
                alert("Failed to load course details");
                navigate('/admin/courses');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [courseId, navigate]);

    const handleIntroVideoChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setIntroVideo(file);
            setIntroVideoPreviewUrl(URL.createObjectURL(file));
        }
    };

    const handleThumbnailChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setThumbnail(file);
            setThumbnailPreviewUrl(URL.createObjectURL(file));
        }
    };

    const removeThumbnail = () => {
        setThumbnail(null);
        setThumbnailPreviewUrl(null);
        setFormData(prev => ({ ...prev, thumbnailUrl: "" }));
    };

    const handleMentorImageChange = (index, file) => {
        if (!file) return;
        setMentors(prev => {
            const next = [...prev];
            next[index] = {
                ...next[index],
                imageFile: file,
                imagePreviewUrl: URL.createObjectURL(file)
            };
            return next;
        });
    };

    const removeMentorAtIndex = (index) => {
        setMentors(prev => prev.filter((_, i) => i !== index));
    };

    const removeIntroVideo = () => {
        setIntroVideo(null);
        setIntroVideoPreviewUrl(null);
        setFormData(prev => ({ ...prev, introVideoUrl: "" }));
    };

    const handleCertificateChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setCertificate(file);
            setCertificatePreviewUrl(URL.createObjectURL(file));
        }
    };

    const removeCertificate = () => {
        setCertificate(null);
        setCertificatePreviewUrl(null);
        setFormData(prev => ({ ...prev, certificateUrl: "" }));
    };

    const handleBrochureChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setBrochure(file);
        }
    };

    const removeBrochure = () => {
        setBrochure(null);
    };

    const deleteExistingBrochure = async () => {
        if (!window.confirm("Are you sure you want to delete the brochure?")) return;
        try {
            await api.delete(`/admin/courses/${courseId}/brochure`);
            setBrochureUrl(null);
            alert("Brochure deleted successfully!");
        } catch (error) {
            console.error("Failed to delete brochure", error);
            alert("Failed to delete brochure");
        }
    };

    const handleToolsCoveredChange = (e) => {
        const files = Array.from(e.target.files || []);
        if (files.length === 0) return;
        const previews = files.map(file => URL.createObjectURL(file));
        setToolsCoveredFiles(prev => [...prev, ...files]);
        setToolsCoveredPreviews(prev => [...prev, ...previews]);
        e.target.value = '';
    };

    const removeToolsCoveredAtIndex = (index) => {
        setToolsCoveredFiles(prev => prev.filter((_, i) => i !== index));
        setToolsCoveredPreviews(prev => prev.filter((_, i) => i !== index));
    };

    const deleteExistingTool = async (index) => {
        const response = await api.delete(`/admin/courses/${courseId}/tools-covered/${index}`);
        if (response?.data?.toolsCovered) {
            setExistingToolsCovered(response.data.toolsCovered);
        } else {
            setExistingToolsCovered(prev => prev.filter((_, i) => i !== index));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            // 1. Update Course Metadata
            const cleanFormData = {
                ...formData,
                price: formData.price === '' ? null : parseFloat(formData.price),
                originalPrice: formData.originalPrice === '' ? null : parseFloat(formData.originalPrice),
                discount: formData.discount === '' ? 0 : parseInt(formData.discount, 10),
            };

            // Filter out empty skills
            const cleanSkills = skills.filter(skill => skill.trim() !== "");

            // Filter out empty highlight items
            const cleanKeyHighlights = keyHighlights.map(({ title, items }) => ({
                title,
                items: (items || []).filter(item => item.trim() !== "")
            }));

            // Filter out empty career opportunities
            const cleanCareerOpportunities = careerOpportunities.filter(
                op => op.title.trim() !== "" || op.description.trim() !== ""
            );

            const cleanFaqs = faqs
                .map(f => ({ question: (f.question || "").trim(), answer: (f.answer || "").trim() }))
                .filter(f => f.question !== "" && f.answer !== "");

            const mentorsWithKeys = [...mentors];
            for (let i = 0; i < mentorsWithKeys.length; i++) {
                const m = mentorsWithKeys[i];
                if (!m?.imageFile) continue;
                const uploadData = new FormData();
                uploadData.append('file', m.imageFile);
                const resp = await api.post(`/admin/courses/${courseId}/mentors/image`, uploadData);
                const key = resp?.data?.key;
                if (key) {
                    mentorsWithKeys[i] = { ...m, imageKey: key, imageFile: null };
                }
            }

            const cleanMentors = mentorsWithKeys
                .map(m => ({
                    name: (m.name || "").trim(),
                    role: (m.role || "").trim(),
                    workExpYears: m.workExpYears === "" ? null : Number(m.workExpYears),
                    teachExpYears: m.teachExpYears === "" ? null : Number(m.teachExpYears),
                    about: (m.about || "").trim(),
                    imageKey: (m.imageKey || "").trim() || null
                }))
                .filter(m =>
                    m.name ||
                    m.role ||
                    m.about ||
                    m.workExpYears !== null ||
                    m.teachExpYears !== null ||
                    m.imageKey
                );

            const payload = {
                ...formData,
                keyHighlights: cleanKeyHighlights,
                skills: cleanSkills,
                careerOpportunities: cleanCareerOpportunities,
                faqs: cleanFaqs,
                mentors: cleanMentors
            };
            // Since we stripped them out if empty, the backend can now process it correctly
            if (payload.thumbnailUrl === "") payload.thumbnailUrl = null;
            if (payload.certificateUrl === "") payload.certificateUrl = null;
            if (payload.introVideoUrl === "") payload.introVideoUrl = null;
            console.log("Sending payload:", JSON.stringify(payload, null, 2));

            await api.put(`/admin/courses/${courseId}`, payload);

            // 2. Upload Thumbnail if selected (for listing cards)
            if (thumbnail) {
                const uploadData = new FormData();
                uploadData.append('file', thumbnail);
                const response = await api.post(`/admin/courses/${courseId}/thumbnail`, uploadData);
                if (response?.data?.thumbnailUrl) {
                    setFormData(prev => ({ ...prev, thumbnailUrl: response.data.thumbnailUrl }));
                    setThumbnailPreviewUrl(response.data.thumbnailUrl);
                }
                setThumbnail(null);
            }

            // 3. Upload Intro Video if selected
            if (introVideo) {
                const uploadData = new FormData();
                uploadData.append('file', introVideo);
                const response = await api.post(`/admin/courses/${courseId}/intro-video`, uploadData);
                if (response?.data?.introVideoUrl) {
                    setFormData(prev => ({ ...prev, introVideoUrl: response.data.introVideoUrl }));
                    setIntroVideoPreviewUrl(response.data.introVideoUrl);
                }
                setIntroVideo(null);
            }

            // 4. Upload Certificate if selected
            if (certificate) {
                const uploadData = new FormData();
                uploadData.append('file', certificate);
                const response = await api.post(`/admin/courses/${courseId}/certificate`, uploadData);
                if (response?.data?.certificateUrl) {
                    setFormData(prev => ({ ...prev, certificateUrl: response.data.certificateUrl }));
                    setCertificatePreviewUrl(response.data.certificateUrl);
                }
                setCertificate(null);
            }

            // 5. Upload Brochure if selected
            if (brochure) {
                const uploadData = new FormData();
                uploadData.append('file', brochure);
                const response = await api.post(`/admin/courses/${courseId}/brochure`, uploadData);
                if (response?.data?.brochureUrl) {
                    setBrochureUrl(response.data.brochureUrl);
                }
                setBrochure(null);
            }

            // 6. Upload tools covered logos if selected
            if (toolsCoveredFiles.length > 0) {
                const toolsData = new FormData();
                toolsCoveredFiles.forEach((file) => {
                    toolsData.append('files', file);
                });
                const response = await api.post(`/admin/courses/${courseId}/tools-covered?append=true`, toolsData);
                if (response?.data?.toolsCovered) {
                    setExistingToolsCovered(response.data.toolsCovered);
                }
                setToolsCoveredFiles([]);
                setToolsCoveredPreviews([]);
            }

            alert("Course updated successfully!");
            navigate('/admin/courses');
        } catch (error) {
            console.error("Failed to update course", error);
            if (error.response) {
                console.error("Server response:", error.response.data);
                alert(`Failed to update course: ${error.response.data.message || error.message}`);
            } else {
                alert("Failed to update course: " + error.message);
            }
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <DashboardLayout>
                <div className="flex items-center justify-center min-h-[400px]">
                    <Loader2 className="animate-spin text-indigo-600" size={40} />
                </div>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout>
            <div className="max-w-3xl mx-auto">
                <button
                    onClick={() => navigate('/admin/courses')}
                    className="flex items-center gap-2 text-gray-500 hover:text-gray-800 mb-6 transition-colors"
                >
                    <ArrowLeft size={18} /> Back to Courses
                </button>

                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                    <div className="flex justify-between items-center mb-8">
                        <h1 className="text-2xl font-bold text-gray-800">Edit Course</h1>
                        <span className="text-sm text-gray-500">ID: {courseId}</span>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Course Title</label>
                            <input
                                type="text"
                                required
                                className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                placeholder="e.g. Master Data Science 2024"
                                value={formData.title}
                                onChange={e => setFormData({ ...formData, title: e.target.value })}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Description</label>
                            <textarea
                                required
                                className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 min-h-[120px]"
                                placeholder="Detailed course description..."
                                value={formData.description}
                                onChange={e => setFormData({ ...formData, description: e.target.value })}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Job Benefit</label>
                            <select
                                required
                                className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 bg-white"
                                value={formData.subtitle}
                                onChange={e => setFormData({ ...formData, subtitle: e.target.value })}
                            >
                                <option value="100% Job Guarantee (T&C Apply)">100% Job Guarantee (T&C Apply)</option>
                                <option value="100% Job Assistance (T&C Apply)">100% Job Assistance (T&C Apply)</option>
                            </select>
                        </div>

                        <div className="grid grid-cols-2 gap-6">
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Price (₹)</label>
                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    required
                                    className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                    placeholder="4999"
                                    value={formData.price}
                                    onChange={e => setFormData({ ...formData, price: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Original Price (₹)</label>
                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                    placeholder="Optional"
                                    value={formData.originalPrice}
                                    onChange={e => setFormData({ ...formData, originalPrice: e.target.value })}
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-6">
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Duration</label>
                                <input
                                    type="text"
                                    required
                                    className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                    placeholder="e.g. 6 Months"
                                    value={formData.duration}
                                    onChange={e => setFormData({ ...formData, duration: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Discount (%)</label>
                                <input
                                    type="number"
                                    min="0"
                                    step="1"
                                    className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                                    placeholder="0"
                                    value={formData.discount}
                                    onChange={e => setFormData({ ...formData, discount: e.target.value })}
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-6">
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Category</label>
                                <select
                                    required
                                    className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 bg-white"
                                    value={formData.category}
                                    onChange={e => setFormData({ ...formData, category: e.target.value })}
                                >
                                    <option value="" disabled>Select Category</option>
                                    {categories.map(cat => (
                                        <option key={cat.id} value={cat.name}>{cat.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-semibold text-gray-700 mb-2">Mode</label>
                                <select
                                    className="w-full border border-gray-300 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 bg-white"
                                    value={formData.mode}
                                    onChange={e => setFormData({ ...formData, mode: e.target.value })}
                                >
                                    <option value="Online">Online</option>
                                    <option value="Offline">Offline</option>
                                    <option value="Online/Offline">Online/Offline</option>
                                </select>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Course Thumbnail</label>
                            <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-xl hover:border-indigo-500 transition-colors bg-gray-50/50">
                                {thumbnailPreviewUrl ? (
                                    <div className="relative w-full aspect-video max-w-sm">
                                        <img
                                            src={thumbnailPreviewUrl}
                                            alt="Thumbnail Preview"
                                            className="w-full h-full object-cover rounded-lg shadow-sm"
                                        />
                                        <button
                                            type="button"
                                            onClick={removeThumbnail}
                                            className="absolute -top-2 -right-2 bg-red-500 text-white p-1.5 rounded-full shadow-md hover:bg-red-600 transition-colors"
                                        >
                                            <X size={14} />
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-1 text-center">
                                        <ImageIcon className="mx-auto h-12 w-12 text-gray-400" />
                                        <div className="flex text-sm text-gray-600">
                                            <label className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none">
                                                <span>Upload a file</span>
                                                <input
                                                    type="file"
                                                    className="sr-only"
                                                    accept="image/*"
                                                    onChange={handleThumbnailChange}
                                                />
                                            </label>
                                            <p className="pl-1">or drag and drop</p>
                                        </div>
                                        <p className="text-xs text-gray-500">PNG, JPG, GIF up to 5MB</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Course Video</label>
                            <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-xl hover:border-indigo-500 transition-colors bg-gray-50/50">
                                {introVideoPreviewUrl ? (
                                    <div className="relative w-full aspect-video max-w-sm">
                                        <video src={introVideoPreviewUrl} className="w-full h-full rounded-lg shadow-sm bg-black" controls />
                                        <button
                                            type="button"
                                            onClick={removeIntroVideo}
                                            className="absolute -top-2 -right-2 bg-red-500 text-white p-1.5 rounded-full shadow-md hover:bg-red-600 transition-colors"
                                        >
                                            <X size={14} />
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-1 text-center">
                                        <ImageIcon className="mx-auto h-12 w-12 text-gray-400" />
                                        <div className="flex text-sm text-gray-600">
                                            <label className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none">
                                                <span>Upload a file</span>
                                                <input
                                                    type="file"
                                                    className="sr-only"
                                                    accept="video/*"
                                                    onChange={handleIntroVideoChange}
                                                />
                                            </label>
                                            <p className="pl-1">or drag and drop</p>
                                        </div>
                                        <p className="text-xs text-gray-500">MP4, MOV, WEBM up to 500MB</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Course Brochure (PDF)</label>
                            <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-xl hover:border-indigo-500 transition-colors bg-gray-50/50">
                                {brochure || brochureUrl ? (
                                    <div className="flex items-center gap-4 bg-white p-4 rounded-lg shadow-sm w-full max-w-sm">
                                        <div className="bg-indigo-50 p-2 rounded-lg text-indigo-600">
                                            <Upload size={24} />
                                        </div>
                                        <div className="flex-1 truncate">
                                            <p className="text-sm font-medium text-gray-900 truncate">
                                                {brochure ? brochure.name : "Existing Brochure"}
                                            </p>
                                            <p className="text-xs text-gray-500">
                                                {brochure ? `${(brochure.size / 1024 / 1024).toFixed(2)} MB` : (
                                                    <a
                                                        href={brochureUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="text-indigo-600 hover:underline"
                                                    >
                                                        View Current Brochure
                                                    </a>
                                                )}
                                            </p>
                                        </div>
                                        <button
                                            type="button"
                                            onClick={brochure ? removeBrochure : deleteExistingBrochure}
                                            className="text-gray-400 hover:text-red-500 transition-colors"
                                        >
                                            <X size={18} />
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-1 text-center">
                                        <Upload className="mx-auto h-12 w-12 text-gray-400" />
                                        <div className="flex text-sm text-gray-600">
                                            <label className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none">
                                                <span>Upload a file</span>
                                                <input
                                                    type="file"
                                                    className="sr-only"
                                                    accept=".pdf"
                                                    onChange={handleBrochureChange}
                                                />
                                            </label>
                                            <p className="pl-1">or drag and drop</p>
                                        </div>
                                        <p className="text-xs text-gray-500">PDF up to 10MB</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Certificate Image</label>
                            <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-xl hover:border-indigo-500 transition-colors bg-gray-50/50">
                                {certificatePreviewUrl ? (
                                    <div className="relative w-full aspect-video max-w-sm">
                                        <img
                                            src={certificatePreviewUrl}
                                            alt="Certificate Preview"
                                            className="w-full h-full object-contain rounded-lg shadow-sm bg-white"
                                        />
                                        <button
                                            type="button"
                                            onClick={removeCertificate}
                                            className="absolute -top-2 -right-2 bg-red-500 text-white p-1.5 rounded-full shadow-md hover:bg-red-600 transition-colors"
                                        >
                                            <X size={14} />
                                        </button>
                                    </div>
                                ) : (
                                    <div className="space-y-1 text-center">
                                        <ImageIcon className="mx-auto h-12 w-12 text-gray-400" />
                                        <div className="flex text-sm text-gray-600">
                                            <label className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none">
                                                <span>Upload a file</span>
                                                <input
                                                    type="file"
                                                    className="sr-only"
                                                    accept="image/*"
                                                    onChange={handleCertificateChange}
                                                />
                                            </label>
                                            <p className="pl-1">or drag and drop</p>
                                        </div>
                                        <p className="text-xs text-gray-500">PNG, JPG, SVG up to 5MB</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Tools Covered (Logos)</label>
                            <div className="mt-1 px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-xl hover:border-indigo-500 transition-colors bg-gray-50/50">
                                <div className="space-y-3 text-center">
                                    <div className="flex items-center justify-center">
                                        <label className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none">
                                            <span>Upload images</span>
                                            <input
                                                type="file"
                                                className="sr-only"
                                                accept="image/*"
                                                multiple
                                                onChange={handleToolsCoveredChange}
                                            />
                                        </label>
                                    </div>
                                    <p className="text-xs text-gray-500">PNG, JPG, SVG up to 5MB each</p>
                                </div>

                                {existingToolsCovered.length > 0 ? (
                                    <div className="mt-5 grid grid-cols-2 sm:grid-cols-3 gap-4">
                                        {existingToolsCovered.map((src, index) => (
                                            <div key={index} className="relative aspect-video bg-white rounded-xl border border-gray-200 overflow-hidden">
                                                <img src={src} alt="Tool" className="w-full h-full object-contain p-3" />
                                                <button
                                                    type="button"
                                                    onClick={() => deleteExistingTool(index)}
                                                    className="absolute top-2 right-2 bg-red-500 text-white p-1.5 rounded-full shadow-md hover:bg-red-600 transition-colors"
                                                >
                                                    <X size={14} />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                ) : null}

                                {toolsCoveredPreviews.length > 0 ? (
                                    <div className="mt-5 grid grid-cols-2 sm:grid-cols-3 gap-4">
                                        {toolsCoveredPreviews.map((src, index) => (
                                            <div key={index} className="relative aspect-video bg-white rounded-xl border border-gray-200 overflow-hidden">
                                                <img src={src} alt="Tool Preview" className="w-full h-full object-contain p-3" />
                                                <button
                                                    type="button"
                                                    onClick={() => removeToolsCoveredAtIndex(index)}
                                                    className="absolute top-2 right-2 bg-red-500 text-white p-1.5 rounded-full shadow-md hover:bg-red-600 transition-colors"
                                                >
                                                    <X size={14} />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                ) : null}
                            </div>
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-4">
                                <label className="block text-sm font-semibold text-gray-700">Mentors</label>
                                <button
                                    type="button"
                                    onClick={() => setMentors([...mentors, { name: "", role: "", workExpYears: "", teachExpYears: "", about: "", imageKey: "", imageUrl: "", imageFile: null, imagePreviewUrl: "" }])}
                                    className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                                >
                                    + Add Mentor
                                </button>
                            </div>

                            <div className="space-y-4">
                                {mentors.map((mentor, index) => (
                                    <div key={index} className="p-4 border border-gray-200 rounded-xl bg-gray-50/50 relative">
                                        <button
                                            type="button"
                                            onClick={() => removeMentorAtIndex(index)}
                                            className="absolute top-2 right-2 text-gray-400 hover:text-red-500"
                                        >
                                            <X size={18} />
                                        </button>

                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                            <input
                                                type="text"
                                                placeholder="Name"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                                value={mentor.name}
                                                onChange={(e) => {
                                                    const next = [...mentors];
                                                    next[index] = { ...next[index], name: e.target.value };
                                                    setMentors(next);
                                                }}
                                            />
                                            <input
                                                type="text"
                                                placeholder="Role / Title"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                                value={mentor.role}
                                                onChange={(e) => {
                                                    const next = [...mentors];
                                                    next[index] = { ...next[index], role: e.target.value };
                                                    setMentors(next);
                                                }}
                                            />
                                            <input
                                                type="number"
                                                min="0"
                                                placeholder="Work Exp (years)"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                                value={mentor.workExpYears}
                                                onChange={(e) => {
                                                    const next = [...mentors];
                                                    next[index] = { ...next[index], workExpYears: e.target.value };
                                                    setMentors(next);
                                                }}
                                            />
                                            <input
                                                type="number"
                                                min="0"
                                                placeholder="Teaching Exp (years)"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                                value={mentor.teachExpYears}
                                                onChange={(e) => {
                                                    const next = [...mentors];
                                                    next[index] = { ...next[index], teachExpYears: e.target.value };
                                                    setMentors(next);
                                                }}
                                            />
                                        </div>

                                        <div className="mt-3">
                                            <textarea
                                                placeholder="About"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm min-h-[90px]"
                                                value={mentor.about}
                                                onChange={(e) => {
                                                    const next = [...mentors];
                                                    next[index] = { ...next[index], about: e.target.value };
                                                    setMentors(next);
                                                }}
                                            />
                                        </div>

                                        <div className="mt-3">
                                            <label className="block text-xs font-semibold text-gray-600 mb-2">Mentor Image</label>
                                            <div className="flex items-center gap-3">
                                                {mentor.imagePreviewUrl ? (
                                                    <img
                                                        src={mentor.imagePreviewUrl}
                                                        alt="Mentor"
                                                        className="w-16 h-16 rounded-full object-cover border border-gray-200 bg-white"
                                                    />
                                                ) : (
                                                    <div className="w-16 h-16 rounded-full bg-white border border-gray-200 flex items-center justify-center text-gray-400">
                                                        <ImageIcon size={22} />
                                                    </div>
                                                )}
                                                <input
                                                    type="file"
                                                    accept="image/*"
                                                    onChange={(e) => handleMentorImageChange(index, e.target.files?.[0])}
                                                    className="block w-full text-sm text-gray-500
                                                      file:mr-4 file:py-2 file:px-4
                                                      file:rounded-full file:border-0
                                                      file:text-sm file:font-semibold
                                                      file:bg-indigo-50 file:text-indigo-700
                                                      hover:file:bg-indigo-100"
                                                />
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Skills Section */}
                        <div>
                            <div className="flex justify-between items-center mb-4">
                                <label className="block text-sm font-semibold text-gray-700">Skills Covered</label>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                {skills.map((skill, index) => (
                                    <div key={index} className="flex gap-2">
                                        <input
                                            type="text"
                                            placeholder="e.g. Python"
                                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                            value={skill}
                                            onChange={(e) => {
                                                const newSkills = [...skills];
                                                newSkills[index] = e.target.value;
                                                setSkills(newSkills);
                                            }}
                                        />
                                        <button
                                            type="button"
                                            onClick={() => {
                                                const newSkills = skills.filter((_, i) => i !== index);
                                                setSkills(newSkills);
                                            }}
                                            className="text-gray-400 hover:text-red-500"
                                        >
                                            <X size={18} />
                                        </button>
                                    </div>
                                ))}
                            </div>
                            <button
                                type="button"
                                onClick={() => setSkills([...skills, ""])}
                                className="mt-3 text-sm text-indigo-600 font-medium hover:text-indigo-800"
                            >
                                + Add Skill
                            </button>
                        </div>

                        {/* Key Highlights Section */}
                        <div>
                            <div className="flex justify-between items-center mb-4">
                                <label className="block text-sm font-semibold text-gray-700">Key Highlights</label>
                                <button
                                    type="button"
                                    onClick={() => setKeyHighlights([...keyHighlights, { title: "", items: [""] }])}
                                    className="text-sm text-indigo-600 font-medium hover:text-indigo-800"
                                >
                                    + Add Card
                                </button>
                            </div>

                            <div className="space-y-6">
                                {keyHighlights.map((card, cardIndex) => (
                                    <div key={cardIndex} className="p-4 border border-gray-200 rounded-xl bg-gray-50">
                                        <div className="flex justify-between mb-3">
                                            <input
                                                type="text"
                                                placeholder="Card Title (e.g. Product)"
                                                className="w-full mr-2 border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                                value={card.title}
                                                onChange={(e) => {
                                                    const newHighlights = [...keyHighlights];
                                                    newHighlights[cardIndex].title = e.target.value;
                                                    setKeyHighlights(newHighlights);
                                                }}
                                            />
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    const newHighlights = keyHighlights.filter((_, i) => i !== cardIndex);
                                                    setKeyHighlights(newHighlights);
                                                }}
                                                className="text-red-500 hover:text-red-700"
                                            >
                                                <X size={18} />
                                            </button>
                                        </div>

                                        <div className="pl-4 border-l-2 border-indigo-100 space-y-2">
                                            {card.items.map((item, itemIndex) => (
                                                <div key={itemIndex} className="flex gap-2">
                                                    <input
                                                        type="text"
                                                        placeholder="Highlight Item"
                                                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                                                        value={item}
                                                        onChange={(e) => {
                                                            const newHighlights = [...keyHighlights];
                                                            newHighlights[cardIndex].items[itemIndex] = e.target.value;
                                                            setKeyHighlights(newHighlights);
                                                        }}
                                                    />
                                                    <button
                                                        type="button"
                                                        onClick={() => {
                                                            const newHighlights = [...keyHighlights];
                                                            newHighlights[cardIndex].items = newHighlights[cardIndex].items.filter((_, i) => i !== itemIndex);
                                                            setKeyHighlights(newHighlights);
                                                        }}
                                                        className="text-gray-400 hover:text-red-500"
                                                    >
                                                        <X size={14} />
                                                    </button>
                                                </div>
                                            ))}
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    const newHighlights = [...keyHighlights];
                                                    newHighlights[cardIndex].items.push("");
                                                    setKeyHighlights(newHighlights);
                                                }}
                                                className="text-xs text-indigo-600 font-medium hover:text-indigo-800"
                                            >
                                                + Add Item
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Career Opportunities Section */}
                        <div>
                            <div className="flex justify-between items-center mb-4">
                                <label className="block text-sm font-semibold text-gray-700">Career Opportunities (Role Cards)</label>
                                <button
                                    type="button"
                                    onClick={() => setCareerOpportunities([...careerOpportunities, { title: "", description: "" }])}
                                    className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                                >
                                    + Add Role
                                </button>
                            </div>

                            <div className="grid grid-cols-1 gap-4">
                                {careerOpportunities.map((op, index) => (
                                    <div key={index} className="p-4 border border-gray-200 rounded-xl bg-gray-50/50 relative">
                                        <button
                                            type="button"
                                            onClick={() => {
                                                const newOps = careerOpportunities.filter((_, i) => i !== index);
                                                setCareerOpportunities(newOps);
                                            }}
                                            className="absolute top-2 right-2 text-gray-400 hover:text-red-500"
                                        >
                                            <X size={18} />
                                        </button>
                                        <div className="space-y-3">
                                            <input
                                                type="text"
                                                placeholder="Role Title (e.g. Data Scientist)"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-medium"
                                                value={op.title}
                                                onChange={(e) => {
                                                    const newOps = [...careerOpportunities];
                                                    newOps[index].title = e.target.value;
                                                    setCareerOpportunities(newOps);
                                                }}
                                            />
                                            <textarea
                                                placeholder="Role Description"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm min-h-[80px]"
                                                value={op.description}
                                                onChange={(e) => {
                                                    const newOps = [...careerOpportunities];
                                                    newOps[index].description = e.target.value;
                                                    setCareerOpportunities(newOps);
                                                }}
                                            />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-4">
                                <label className="block text-sm font-semibold text-gray-700">Frequently Asked Questions</label>
                                <button
                                    type="button"
                                    onClick={() => setFaqs([...faqs, { question: "", answer: "" }])}
                                    className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                                >
                                    + Add FAQ
                                </button>
                            </div>

                            <div className="space-y-4">
                                {faqs.map((faq, index) => (
                                    <div key={index} className="p-4 border border-gray-200 rounded-xl bg-gray-50/50 relative">
                                        <button
                                            type="button"
                                            onClick={() => setFaqs(faqs.filter((_, i) => i !== index))}
                                            className="absolute top-2 right-2 text-gray-400 hover:text-red-500"
                                        >
                                            <X size={18} />
                                        </button>
                                        <div className="space-y-3">
                                            <input
                                                type="text"
                                                placeholder="Question"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-medium"
                                                value={faq.question}
                                                onChange={(e) => {
                                                    const next = [...faqs];
                                                    next[index] = { ...next[index], question: e.target.value };
                                                    setFaqs(next);
                                                }}
                                            />
                                            <textarea
                                                placeholder="Answer"
                                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm min-h-[80px]"
                                                value={faq.answer}
                                                onChange={(e) => {
                                                    const next = [...faqs];
                                                    next[index] = { ...next[index], answer: e.target.value };
                                                    setFaqs(next);
                                                }}
                                            />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div className="pt-4 flex justify-end">
                            <button
                                type="submit"
                                disabled={submitting}
                                className="flex items-center gap-2 bg-indigo-600 text-white px-8 py-3 rounded-xl hover:bg-indigo-700 font-medium disabled:opacity-70"
                            >
                                <Save size={20} />
                                {submitting ? 'Updating...' : 'Update Course'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </DashboardLayout>
    );
};

export default CourseEdit;

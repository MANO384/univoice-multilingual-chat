export const COUNTRY_CODES = [
  { code: '+91', flag: '🇮🇳', name: 'India (+91)' },
  { code: '+1', flag: '🇺🇸', name: 'USA / Canada (+1)' },
  { code: '+44', flag: '🇬🇧', name: 'UK (+44)' },
  { code: '+81', flag: '🇯🇵', name: 'Japan (+81)' },
  { code: '+49', flag: '🇩🇪', name: 'Germany (+49)' },
  { code: '+33', flag: '🇫🇷', name: 'France (+33)' },
  { code: '+34', flag: '🇪🇸', name: 'Spain (+34)' },
  { code: '+971', flag: '🇦🇪', name: 'UAE (+971)' },
  { code: '+65', flag: '🇸🇬', name: 'Singapore (+65)' },
  { code: '+82', flag: '🇰🇷', name: 'South Korea (+82)' },
  { code: '+55', flag: '🇧🇷', name: 'Brazil (+55)' },
  { code: '+61', flag: '🇦🇺', name: 'Australia (+61)' },
  { code: '+60', flag: '🇲🇾', name: 'Malaysia (+60)' },
  { code: '+966', flag: '🇸🇦', name: 'Saudi Arabia (+966)' }
];

export const ALL_LANGUAGES = [
  // Indian Regional Languages (Sarvam AI)
  { code: 'ta', label: 'Tamil (தமிழ்)' },
  { code: 'te', label: 'Telugu (తెలుగు)' },
  { code: 'hi', label: 'Hindi (हिन्दी)' },
  { code: 'kn', label: 'Kannada (ಕನ್ನಡ)' },
  { code: 'ml', label: 'Malayalam (മലയാളം)' },
  { code: 'bn', label: 'Bengali (বাংলা)' },
  { code: 'mr', label: 'Marathi (मराठी)' },
  { code: 'gu', label: 'Gujarati (ગુજરાતી)' },
  { code: 'pa', label: 'Punjabi (ਪੰਜਾਬੀ)' },
  { code: 'od', label: 'Odia (ଓଡ଼ିଆ)' },
  { code: 'en', label: 'English' },

  // Global Foreign Languages (OpenAI / Global AI + English Pivot)
  { code: 'es', label: 'Spanish (Español)' },
  { code: 'fr', label: 'French (Français)' },
  { code: 'de', label: 'German (Deutsch)' },
  { code: 'ja', label: 'Japanese (日本語)' },
  { code: 'ko', label: 'Korean (한국어)' },
  { code: 'ar', label: 'Arabic (العربية)' },
  { code: 'zh', label: 'Chinese (中文)' },
  { code: 'pt', label: 'Portuguese (Português)' },
  { code: 'ru', label: 'Russian (Русский)' },
  { code: 'it', label: 'Italian (Italiano)' },
  { code: 'tr', label: 'Turkish (Türkçe)' },
  { code: 'vi', label: 'Vietnamese (Tiếng Việt)' },
  { code: 'th', label: 'Thai (ไทย)' },
  { code: 'id', label: 'Indonesian (Bahasa Indonesia)' }
];

export const CONTACT_LANG_OPTIONS = [
  { code: 'DEFAULT', label: 'My Default Language' },
  { code: 'NONE', label: 'Unchanged (No Translation)' },
  ...ALL_LANGUAGES
];
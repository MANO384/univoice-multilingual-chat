import React, { useState } from 'react';
import { api } from '../services/api';
import { MessageSquare, Lock, User, Globe, KeyRound, ArrowLeft } from 'lucide-react';

import { COUNTRY_CODES, ALL_LANGUAGES } from '../constants/languages';

export default function AuthScreen({ onAuthSuccess }) {
  const [mode, setMode] = useState('login'); // 'login' | 'signup' | 'forgot'
  const [step, setStep] = useState(1);

  const [countryCode, setCountryCode] = useState('+91');
  const [localPhone, setLocalPhone] = useState('');
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [motherTongue, setMotherTongue] = useState('ta');
  const [preferredLanguage, setPreferredLanguage] = useState('ta');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  // Combines selected country code + digits cleanly
  const getFullPhoneNumber = () => {
    const cleanDigits = localPhone.replace(/\D/g, '');
    return `${countryCode}${cleanDigits}`;
  };

  const switchMode = (newMode) => {
    setMode(newMode);
    setStep(1);
    setOtp('');
    setPassword('');
    setError('');
    setInfo('');
  };

  const renderPhoneInput = (label = 'Phone Number') => (
    <div>
      <label className="text-xs text-[#8696a0] block mb-1">{label}</label>
      <div className="flex gap-2">
        <select
          value={countryCode}
          onChange={(e) => setCountryCode(e.target.value)}
          className="bg-[#202c33] border border-[#2a3942] rounded-lg px-2.5 py-2 text-sm text-white focus:outline-none focus:border-[#00a884] shrink-0"
        >
          {COUNTRY_CODES.map((c) => (
            <option key={c.code} value={c.code}>
              {c.flag} {c.code}
            </option>
          ))}
        </select>
        <input
          type="tel"
          required
          maxLength={15}
          value={localPhone}
          onChange={(e) => setLocalPhone(e.target.value.replace(/\D/g, ''))}
          placeholder="Enter phone number"
          className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white tracking-wide focus:outline-none focus:border-[#00a884]"
/>
      </div>
    </div>
  );

  // LOGIN
  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/login', {
        phoneNumber: getFullPhoneNumber(),
        password
      });
      const data = res.data;
      sessionStorage.setItem('univoice_access_token', data.accessToken);
      sessionStorage.setItem('univoice_refresh_token', data.refreshToken);
      sessionStorage.setItem('univoice_user', JSON.stringify(data));
      onAuthSuccess(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid phone number or password');
    } finally {
      setLoading(false);
    }
  };

  // SIGN UP STEP 1: SEND OTP
  const handleSendSignupOtp = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    setLoading(true);
    try {
      await api.post('/auth/send-otp', { phoneNumber: getFullPhoneNumber() });
      setInfo(`OTP sent for ${getFullPhoneNumber()}! Check your user-service console.`);
      setStep(2);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  // SIGN UP STEP 2: VERIFY OTP
  const handleVerifySignupOtp = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/auth/verify-otp', {
        phoneNumber: getFullPhoneNumber(),
        otp
      });
      setInfo('OTP verified! Complete your profile below.');
      setStep(3);
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid or expired OTP');
    } finally {
      setLoading(false);
    }
  };

  // SIGN UP STEP 3: COMPLETE REGISTRATION
  const handleCompleteSignup = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/signup', {
        phoneNumber: getFullPhoneNumber(),
        otp,
        password,
        displayName,
        motherTongue,
        preferredLanguage
      });
      const data = res.data;
      sessionStorage.setItem('univoice_access_token', data.accessToken);
      sessionStorage.setItem('univoice_refresh_token', data.refreshToken);
      sessionStorage.setItem('univoice_user', JSON.stringify(data));
      onAuthSuccess(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  // FORGOT PASSWORD STEP 1: REQUEST RESET OTP
  const handleSendResetOtp = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    setLoading(true);
    try {
      await api.post('/auth/forgot-password', { phoneNumber: getFullPhoneNumber() });
      setInfo(`Reset OTP generated for ${getFullPhoneNumber()}! Check user-service console.`);
      setStep(2);
    } catch (err) {
      setError(err.response?.data?.message || 'Phone number not registered');
    } finally {
      setLoading(false);
    }
  };

  // FORGOT PASSWORD STEP 2: RESET PASSWORD
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/auth/reset-password', {
        phoneNumber: getFullPhoneNumber(),
        otp,
        newPassword: password
      });
      switchMode('login');
      setInfo('Password updated! Sign in with your new password.');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid or expired OTP');
    } finally {
      setLoading(false);
    }
  };   

  return (
    <div className="min-h-screen bg-[#0c1317] flex items-center justify-center p-4 font-sans text-gray-100">
      <div className="w-full max-w-md bg-[#111b21] border border-[#222d34] rounded-2xl p-8 shadow-2xl">
        {/* Brand Header */}
        <div className="flex flex-col items-center mb-6">
          <div className="w-12 h-12 rounded-2xl bg-[#00a884] flex items-center justify-center shadow-lg shadow-emerald-900/40 mb-3">
            <MessageSquare className="text-white" size={26} />
          </div>
          <h1 className="text-xl font-bold text-white tracking-tight">UniVoice</h1>
          <p className="text-xs text-[#8696a0] mt-1">Real-Time Regional Language Messaging</p>
        </div>

        {/* Alerts */}
        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/40 text-rose-300 text-xs">
            {error}
          </div>
        )}
        {info && (
          <div className="mb-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/40 text-emerald-300 text-xs">
            {info}
          </div>
        )}

        {/* MODE 1: LOGIN */}
        {mode === 'login' && (
          <form onSubmit={handleLogin} className="space-y-4">
            {renderPhoneInput('Phone Number')}

            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="text-xs text-[#8696a0]">Password</label>
                <button
                  type="button"
                  onClick={() => switchMode('forgot')}
                  className="text-xs text-[#00a884] hover:underline"
                >
                  Forgot password?
                </button>
              </div>
              <div className="relative">
                <Lock size={15} className="absolute left-3 top-3 text-[#8696a0]" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || localPhone.length < 7}
              className="w-full bg-[#00a884] hover:bg-emerald-600 disabled:opacity-50 text-white font-semibold py-2.5 rounded-lg text-sm transition shadow-md"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>

            <p className="text-center text-xs text-[#8696a0] pt-2">
              New to UniVoice?{' '}
              <button
                type="button"
                onClick={() => switchMode('signup')}
                className="text-[#00a884] font-medium hover:underline"
              >
                Create an account
              </button>
            </p>
          </form>
        )}

        {/* MODE 2: SIGN UP (3 STEPS) */}
        {mode === 'signup' && (
          <>
            {step === 1 && (
              <form onSubmit={handleSendSignupOtp} className="space-y-4">
                {renderPhoneInput('Select Country & Phone Number')}
                <button
                  type="submit"
                  disabled={loading || localPhone.length < 7}
                  className="w-full bg-[#00a884] hover:bg-emerald-600 disabled:opacity-50 text-white font-semibold py-2.5 rounded-lg text-sm transition"
                >
                  {loading ? 'Sending OTP...' : 'Send OTP'}
                </button>
              </form>
            )}

            {step === 2 && (
              <form onSubmit={handleVerifySignupOtp} className="space-y-4">
                <div>
                  <label className="text-xs text-[#8696a0] block mb-1">Enter 6-Digit Console OTP</label>
                  <div className="relative">
                    <KeyRound size={15} className="absolute left-3 top-3 text-[#8696a0]" />
                    <input
                      type="text"
                      required
                      maxLength={6}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value)}
                      placeholder="123456"
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg pl-9 pr-3 py-2 text-sm text-white tracking-widest focus:outline-none focus:border-[#00a884]"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-[#00a884] hover:bg-emerald-600 text-white font-semibold py-2.5 rounded-lg text-sm transition"
                >
                  {loading ? 'Verifying...' : 'Verify OTP'}
                </button>
              </form>
            )}

            {step === 3 && (
              <form onSubmit={handleCompleteSignup} className="space-y-4">
                <div>
                  <label className="text-xs text-[#8696a0] block mb-1">Display Name</label>
                  <div className="relative">
                    <User size={15} className="absolute left-3 top-3 text-[#8696a0]" />
                    <input
                      type="text"
                      required
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder="Your name"
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-[#8696a0] block mb-1">
                      <Globe size={11} className="inline mr-1" />
                      Native Tongue (Typing)
                    </label>
                    <select
                      value={motherTongue}
                      onChange={(e) => {
                        setMotherTongue(e.target.value);
                        setPreferredLanguage(e.target.value);
                      }}
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-2.5 py-2 text-xs text-white focus:outline-none focus:border-[#00a884]"
                    >
                      {ALL_LANGUAGES.map((l) => (
                        <option key={l.code} value={l.code}>{l.label}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="text-xs text-[#8696a0] block mb-1">Preferred View</label>
                    <select
                      value={preferredLanguage}
                      onChange={(e) => setPreferredLanguage(e.target.value)}
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-2.5 py-2 text-xs text-white focus:outline-none focus:border-[#00a884]"
                    >
                      {ALL_LANGUAGES.map((l) => (
                        <option key={l.code} value={l.code}>{l.label}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="text-xs text-[#8696a0] block mb-1">Create Password</label>
                  <div className="relative">
                    <Lock size={15} className="absolute left-3 top-3 text-[#8696a0]" />
                    <input
                      type="password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-[#00a884] hover:bg-emerald-600 text-white font-semibold py-2.5 rounded-lg text-sm transition"
                >
                  {loading ? 'Creating account...' : 'Complete Sign Up'}
                </button>
              </form>
            )}

            <p className="text-center text-xs text-[#8696a0] pt-4">
              Already have an account?{' '}
              <button
                type="button"
                onClick={() => switchMode('login')}
                className="text-[#00a884] font-medium hover:underline"
              >
                Sign In
              </button>
            </p>
          </>
        )}

        {/* MODE 3: FORGOT PASSWORD */}
        {mode === 'forgot' && (
          <>
            {step === 1 ? (
              <form onSubmit={handleSendResetOtp} className="space-y-4">
                {renderPhoneInput('Registered Phone Number')}
                <button
                  type="submit"
                  disabled={loading || localPhone.length < 10}
                  className="w-full bg-[#00a884] hover:bg-emerald-600 disabled:opacity-50 text-white font-semibold py-2.5 rounded-lg text-sm transition"
                >
                  {loading ? 'Sending Reset OTP...' : 'Send Reset OTP'}
                </button>
              </form>
            ) : (
              <form onSubmit={handleResetPassword} className="space-y-4">
                <div>
                  <label className="text-xs text-[#8696a0] block mb-1">6-Digit Console OTP</label>
                  <div className="relative">
                    <KeyRound size={15} className="absolute left-3 top-3 text-[#8696a0]" />
                    <input
                      type="text"
                      required
                      maxLength={6}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value)}
                      placeholder="123456"
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg pl-9 pr-3 py-2 text-sm text-white tracking-widest focus:outline-none focus:border-[#00a884]"
                    />
                  </div>
                </div>
                <div>
                  <label className="text-xs text-[#8696a0] block mb-1">New Password</label>
                  <div className="relative">
                    <Lock size={15} className="absolute left-3 top-3 text-[#8696a0]" />
                    <input
                      type="password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Enter new password"
                      className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-[#00a884] hover:bg-emerald-600 text-white font-semibold py-2.5 rounded-lg text-sm transition"
                >
                  {loading ? 'Updating...' : 'Reset Password'}
                </button>
              </form>
            )}

            <button
              type="button"
              onClick={() => switchMode('login')}
              className="w-full mt-4 flex items-center justify-center gap-1.5 text-xs text-[#8696a0] hover:text-white transition"
            >
              <ArrowLeft size={14} />
              Back to Sign In
            </button>
          </>
        )}
      </div>
    </div>
  );
}
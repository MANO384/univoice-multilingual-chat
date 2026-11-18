package com.univoice.user.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public final class PhoneNormalizer {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private PhoneNormalizer() {}

    /**
     * Parses and validates any international phone number into standard E.164 format
     * (e.g., "+918239346834", "+14155552671", "+819012345678").
     */
    public static String normalize(String rawPhone, String defaultRegion) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String region = (defaultRegion != null && !defaultRegion.isBlank())
                ? defaultRegion.toUpperCase()
                : "IN";

        try {
            PhoneNumber parsed = PHONE_UTIL.parse(rawPhone.trim(), region);
            if (!PHONE_UTIL.isPossibleNumber(parsed)) {
                throw new IllegalArgumentException("Invalid phone number length for the selected country");
            }
            return PHONE_UTIL.format(parsed, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid international phone number format");
        }
    }

    public static String normalize(String rawPhone) {
        return normalize(rawPhone, "IN");
    }
}
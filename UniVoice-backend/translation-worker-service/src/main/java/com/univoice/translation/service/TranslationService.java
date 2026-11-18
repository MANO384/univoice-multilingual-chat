package com.univoice.translation.service;

import com.univoice.translation.event.RawGroupMessageEvent;
import com.univoice.translation.event.RawMessageEvent;

public interface TranslationService {
    void processRawMessage(RawMessageEvent event);
    void processGroupMessage(RawGroupMessageEvent event);
}

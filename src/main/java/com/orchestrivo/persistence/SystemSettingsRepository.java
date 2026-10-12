package com.orchestrivo.persistence;

import java.util.Optional;

public interface SystemSettingsRepository {
    Optional<String> getSetting(String key);
    void setSetting(String key, String value);
    void removeSetting(String key);
    Optional<Long> getLongSetting(String key);
    void setLongSetting(String key, long value);
}

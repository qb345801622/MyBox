package mara.mybox.data;

import java.io.File;
import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import mara.mybox.tools.ConfigTools;

import mara.mybox.value.Languages;

/**
 * @Author Mara
 * @CreateDate 2019-12-27
 * @License Apache License Version 2.0
 */
public class UserLanguage extends ResourceBundle {

    protected Map<String, String> items;

    public UserLanguage(String name) throws Exception {
        File file = Languages.interfaceLanguageFile(name);
        if (!file.exists()) {
            items = null;
        }
        items = ConfigTools.readValues(file);
    }

    public UserLanguage(Map<String, String> items) {
        this.items = items;
    }

    @Override
    public Object handleGetObject(String key) {
        String value = null;
        if (items != null) {
            value = items.get(key);
        }
        if (value == null) {
            value = Languages.BundleEn.getString(key);
        }
        if (value == null) {
            value = key;
        }
        return value;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Languages.BundleEn.getKeys();
    }

    @Override
    protected Set<String> handleKeySet() {
        return Languages.BundleEn.keySet();
    }
}

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.font;


import java.util.Locale;

/**
 * The Font Manager class.
 * @hide
 */
public class FontManager {

    /**
     * Default Constructor.
     */
    private FontManager() {
        // can't instanciate
    }

    /**
     * Get the selectable fonts information.
     * 
     * @return selectable fonts information
     */
    public static Font[] getSelectableDefaultFonts() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        return nativeGetSelectableDefaultFonts(language);
    }

    /**
     * Get the selected font name.
     * 
     * @return selected font name
     */
    public static String getSelectedDefaultFontName() {
        return nativeGetSelectedDefaultFontName();
    }

    /**
     * Set the selected font name.
     * 
     * @return true - OK / false - NG
     */
    public static boolean setSelectedDefaultFontName(String name) {
        return nativeSetSelectedDefaultFontName(name);
    }

    /**
     * Reset.
     * 
     * @return true - OK / false - NG
     */
    public static boolean reset() {
        return nativeReset();
    }

    /**
     * Font infomation class.
     */
    public static class Font {

        /**
         * The font name.
         */
        private String name;

        /**
         * The font display name.
         */
        private String displayName;

        /**
         * Constructor.
         * 
         * @param name font name
         * @param displayName font display name
         */
        public Font(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        /**
         * Get the font name.
         * 
         * @return font name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Set the font name.
         * 
         * @param name font name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Get the font display name.
         * 
         * @return font display name
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Set the font display name.
         * 
         * @param displayName font display name
         */
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    private static native Font[]  nativeGetSelectableDefaultFonts(String language);
    private static native String  nativeGetSelectedDefaultFontName();
    private static native boolean nativeSetSelectedDefaultFontName(String name);
    private static native boolean nativeReset();
}

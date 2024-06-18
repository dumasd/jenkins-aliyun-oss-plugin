package io.jenkins.plugins.aliyunoss.utils;

public class Utils {

    public static boolean isNullOrEmpty(final String name) {
        return name == null || name.matches("\\s*");
    }

    public static String getFileName(String path) {
        if (isNullOrEmpty(path)) {
            return "";
        }
        int idx = path.lastIndexOf(Constants.C_SLASH);
        if (idx == path.length() - 1) {
            return "";
        }
        return idx < 0 ? path : path.substring(idx);
    }

    public static String removePrefix(String prefix, String file) {
        if (isNullOrEmpty(prefix)) {
            return file;
        }
        int idx = file.indexOf(prefix);
        if (idx == 0) {
            return file.substring(prefix.length());
        }
        return file;
    }

    public static String splicePath(String... ps) {
        StringBuilder builder = new StringBuilder();
        for (String p : ps) {
            if (!isNullOrEmpty(p)) {
                if (builder.length() != 0) {
                    if (builder.toString().endsWith(Constants.SLASH) && p.startsWith(Constants.SLASH)) {
                        builder.append(p.substring(1));
                    } else if (builder.toString().endsWith(Constants.SLASH)) {
                        builder.append(p);
                    } else {
                        builder.append(Constants.SLASH).append(p);
                    }
                } else {
                    builder.append(p);
                }
            }
        }
        return builder.toString();
    }

}

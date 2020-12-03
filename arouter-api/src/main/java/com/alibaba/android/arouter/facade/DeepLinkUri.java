package com.alibaba.android.arouter.facade;

import android.net.Uri;
import android.util.LruCache;

import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.utils.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepLinkUri {
    public static final String componentParamPrefix = "<";

    public static final String componentParamSuffix = ">";

    private LruCache<String, Integer> matchedFixUrl = new LruCache(8);

    private Pattern pattern;

    private HashMap<String, HashMap<String, String>> placeHolderValues;

    private ArrayList<String> placeHolders;

    private String regUrl;

    public RouteMeta routeMeta;

    private String secondaryPath;

    public DeepLinkUri() {
    }

    public DeepLinkUri(String paramString, RouteMeta paramRouteMeta) {
        this.secondaryPath = paramString;
        this.routeMeta = paramRouteMeta;
    }

    private String toFixUrl(String url) {
        int i = url.indexOf("/");
        String str = url;
        if (i != -1)
            str = url.substring(0, TextUtils.delimiterOffset(url, i, url.length(), "?#"));
        return str;
    }

    public Map<String, String> getPlaceHolderValues(String url) {
        url = toFixUrl(url);
        HashMap<String, HashMap<String, String>> cached = this.placeHolderValues;
        return (cached == null) ? null : cached.get(url);
    }

    public String getRegUrl() {
        return this.regUrl;
    }

    public RouteMeta getRouteMeta() {
        return this.routeMeta;
    }

    public boolean isMatcher(Uri paramUri) {
        return isMatcher(paramUri.toString());
    }

    public boolean isMatcher(String rawUrl) {
        if (getRegUrl() == null){
            parse(this.secondaryPath);
        }

        String fixUrl = toFixUrl(rawUrl);
        if (this.matchedFixUrl.get(fixUrl) != null){
            return true;
        }

        if (this.pattern == null){
            this.pattern = Pattern.compile(this.regUrl);
        }

        Matcher matcher = this.pattern.matcher(fixUrl);
        boolean matched = matcher.matches();
        int i = matcher.groupCount();
        if (matched && i > 0 && this.placeHolders != null) {
            if (this.placeHolderValues == null){
                this.placeHolderValues = new HashMap<String, HashMap<String, String>>();
            }
            HashMap<String, String> placeHolders = this.placeHolderValues.get(fixUrl);
            if (placeHolders == null) {
                placeHolders = new HashMap<String, String>();
                this.placeHolderValues.put(fixUrl, placeHolders);
            }
            int j = this.placeHolders.size();
            for (i = 0; i < j; i++){
                placeHolders.put(this.placeHolders.get(i), matcher.group(i + 1));
            }

        }
        if (matched){
            this.matchedFixUrl.put(fixUrl, Integer.valueOf(0));
        }
        return matched;
    }

    public void parse(String path) {
        if (this.regUrl != null){
            return;
        }
        int pathLength = path.length();
        boolean hasScheme = false;
        boolean hasPath = false;
        StringBuilder urlBuilder = new StringBuilder();
        StringBuilder holderBuilder = null;
        if (pathLength > 0) {

            for (int j = 0; j < pathLength; j++) {
                char c = path.charAt(j);
                if (c == '<' || c == '>') {
                    if (c == '<') {
                        holderBuilder = new StringBuilder();
                    } else {
                        if (holderBuilder == null) {
                            throw new IllegalStateException("can't start with >");
                        } else {
                            String holder = holderBuilder.toString();
                            if (this.placeHolders == null) {
                                this.placeHolders = new ArrayList<String>();
                            }
                            this.placeHolders.add(holder);

                            urlBuilder.append("([^/\\s@\\?#]+)");
                            holderBuilder = null;

                        }
                    }
                } else {
                    if (holderBuilder != null) {
                        holderBuilder.append(c);
                    } else {
                        if (c == '/') {
                            if (j > 0) {
                                if (!hasPath && !hasScheme && path.regionMatches(j - 1, "://", 0, 3)) {
                                    hasScheme = true;
                                } else {
                                    hasPath = true;
                                }
                            } else {
                                hasPath = true;
                            }
                        }
                        urlBuilder.append(c);
                    }
                }

            }
            if (!hasScheme) {
                urlBuilder.insert(0, "\\S*");
            }
            if (hasPath) {
                urlBuilder.append("\\S*");
            }
            this.regUrl = urlBuilder.toString();
        }
    }

    public int getPriority(){
        return getRouteMeta().getPriority();
    }
}

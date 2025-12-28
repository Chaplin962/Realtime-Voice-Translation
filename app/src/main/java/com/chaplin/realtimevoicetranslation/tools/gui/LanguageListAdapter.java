

package com.chaplin.realtimevoicetranslation.tools.gui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

import com.chaplin.realtimevoicetranslation.Global;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.tools.CustomLocale;

public class LanguageListAdapter extends BaseAdapter {
    private ArrayList<CustomLocale> languages;
    private CustomLocale selectedLanguage;
    private Activity activity;
    private LayoutInflater inflater;
    private boolean showTTSInfo = true;
    private ArrayList<CustomLocale> ttsLanguages = new ArrayList<>();

    public LanguageListAdapter(Activity activity, ArrayList<CustomLocale> languages, CustomLocale selectedLanguage) {
        this.activity = activity;
        this.languages = languages;
        this.selectedLanguage = selectedLanguage;
        notifyDataSetChanged();
        inflater = activity.getLayoutInflater();
        initializeTTSLanguageList(activity);
    }

    public LanguageListAdapter(Activity activity, boolean showTTSInfo, ArrayList<CustomLocale> languages, CustomLocale selectedLanguage) {
        this.activity = activity;
        this.showTTSInfo = showTTSInfo;
        this.languages = languages;
        this.selectedLanguage = selectedLanguage;
        notifyDataSetChanged();
        inflater = activity.getLayoutInflater();
        initializeTTSLanguageList(activity);
    }

    @Override
    public int getCount() {
        return languages.size();
    }

    @Override
    public Object getItem(int position) {
        return languages.get(position);
    }

    /*public String getItemCode(int position) {
        return languageCodes.get(position);
    }

    public String getItemCode(String item) {
        int index = languageNames.indexOf(item);
        if (index != -1) {
            return languageCodes.get(index);
        } else {
            return "";
        }
    }*/

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        final CustomLocale item = (CustomLocale) getItem(position);
        if (view == null) {
            view = inflater.inflate(R.layout.component_row_language, null);
        }
        if (item.equals(selectedLanguage)) {
            view.findViewById(R.id.isSelected).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.isSelected).setVisibility(View.GONE);
        }
        if(showTTSInfo){
            ((TextView) view.findViewById(R.id.languageName)).setText(item.getDisplayName(ttsLanguages));
        }else {
            ((TextView) view.findViewById(R.id.languageName)).setText(item.getDisplayNameWithoutTTS());
        }
        return view;
    }

    private void initializeTTSLanguageList(Activity activity){
        Global global = (Global) activity.getApplication();
        global.getTTSLanguages(true, new Global.GetLocalesListListener() {
            @Override
            public void onSuccess(ArrayList<CustomLocale> ttsLanguages) {
                LanguageListAdapter.this.ttsLanguages = ttsLanguages;
            }

            @Override
            public void onFailure(int[] reasons, long value) {
                //never called in this case
            }
        });
    }
}

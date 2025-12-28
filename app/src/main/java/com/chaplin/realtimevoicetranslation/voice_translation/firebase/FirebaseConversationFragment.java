

package com.chaplin.realtimevoicetranslation.voice_translation.firebase;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.tools.gui.CustomFragmentPagerAdapter;
import com.chaplin.realtimevoicetranslation.tools.gui.animations.CustomAnimator;
import com.chaplin.realtimevoicetranslation.voice_translation.VoiceTranslationActivity;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode.PairingToolbarFragment;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode._conversation.connection_info.PeersInfoFragment;

import java.util.ArrayList;
import java.util.List;


public class FirebaseConversationFragment extends PairingToolbarFragment {
    private ConstraintLayout constraintLayout;
    private ImageButton exitButton;
    private static final String ARG_TOPIC = "topic";
    private static String username;
    private static String language;
    private TabLayout tabLayout;
    private ViewPager pager;
    private CustomFragmentPagerAdapter pagerAdapter;
    private VoiceTranslationActivity.Callback communicatorCallback;
    private CustomAnimator animator = new CustomAnimator();
    private int pagerPosition = 0;
    private static Bundle args;
    private String TAG = "FirebaseConversationFragment";

    public FirebaseConversationFragment() {
        // Required empty public constructor
    }

    public static FirebaseConversationFragment newInstance(String topic) {
        FirebaseConversationFragment fragment = new FirebaseConversationFragment();
        args = new Bundle();
        args.putString(ARG_TOPIC, topic);
        args.putString("username", username);
        args.putString("language", language);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString("username", "user");
            language = getArguments().getString("language", "en");
            Log.d(TAG, "username received: " + username + ", language: " + language);

        }
        communicatorCallback = new VoiceTranslationActivity.Callback() {
            @Override
            public void onSearchStarted() {
                if (!isLoadingVisible && !isLoadingAnimating) {
                    buttonSearch.setSearching(true, animator);
                }
            }

            @Override
            public void onSearchStopped() {
                if (!isLoadingVisible && !isLoadingAnimating) {
                    buttonSearch.setSearching(false, animator);
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.container);
        exitButton = view.findViewById(R.id.exitButton);
        tabLayout = view.findViewById(R.id.tabsLayout);
        pager = view.findViewById(R.id.tabs);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Toolbar toolbar = activity.findViewById(R.id.toolbarConversation);
        activity.setActionBar(toolbar);

        // we give the constraint layout the information on the system measures (status bar etc.), which the fragmentContainer has, because they are not passed
        // to it if started with a Transaction and therefore it overlaps the status bar because fitsSystemWindows does not work
        WindowInsets windowInsets = activity.getFragmentContainer().getRootWindowInsets();
        if (windowInsets != null) {
            constraintLayout.dispatchApplyWindowInsets(windowInsets.replaceSystemWindowInsets(windowInsets.getSystemWindowInsetLeft(), windowInsets.getSystemWindowInsetTop(), windowInsets.getSystemWindowInsetRight(), 0));
        }

        // Retrieve the topic from arguments
        String topic = null;
        if (getArguments() != null) {
            topic = getArguments().getString(ARG_TOPIC);
        }

        // Insertion of the list of titles
        List<String> titles = new ArrayList<>();
        titles.add(getResources().getString(R.string.conversation));
        titles.add(getResources().getString(R.string.connection));

        // Pass the topic to FirebaseMainConversationFragment
        pagerAdapter = new CustomFragmentPagerAdapter(activity, getChildFragmentManager(), titles, new Fragment[]{FirebaseMainConversationFragment.newInstance(topic, username, language), new PeersInfoFragment()});

        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0 && positionOffset == 0 && pagerPosition == 1) {
                    pagerPosition = 0;
                    ((PeersInfoFragment) pagerAdapter.getFragment(1)).onDeselected();
                    buttonSearch.setVisible(false, null);
                } else if (position == 1 && positionOffset == 0 && pagerPosition == 0) {
                    pagerPosition = 1;
                    ((PeersInfoFragment) pagerAdapter.getFragment(1)).onSelected();
                    if (!isLoadingVisible) {
                        buttonSearch.setVisible(true, null);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        tabLayout.setupWithViewPager(pager);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
            }
        });
    }

    public Fragment getCurrentFragment() {
        int position = pager.getCurrentItem();
        return pagerAdapter.getFragment(position);
    }

    @Override
    public void clearFoundPeers() {
        PeersInfoFragment peersInfoFragment = (PeersInfoFragment) pagerAdapter.getFragment(1);
        if (peersInfoFragment != null) {
            peersInfoFragment.clearFoundPeers();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void startSearch() {
        ((PeersInfoFragment) pagerAdapter.getFragment(1)).startSearch();
    }
}



package com.chaplin.realtimevoicetranslation.tools.gui;

import android.content.Context;
import android.util.AttributeSet;
import com.chaplin.realtimevoicetranslation.R;
import com.chaplin.realtimevoicetranslation.tools.gui.animations.CustomAnimator;

public class KeyFileSelectorButton extends DeactivableButton{
    private Context context;
    private CustomAnimator animator = new CustomAnimator();

    public KeyFileSelectorButton(Context context) {
        super(context);
        this.context = context;
        deactivatedColor = GuiTools.getColorStateList(context, R.color.gray);
        activatedColor = GuiTools.getColorStateList(context, R.color.primary);
        color = activatedColor;
    }

    public KeyFileSelectorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        deactivatedColor = GuiTools.getColorStateList(context, R.color.gray);
        activatedColor = GuiTools.getColorStateList(context, R.color.primary);
        color = activatedColor;
    }

    public KeyFileSelectorButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        deactivatedColor = GuiTools.getColorStateList(context, R.color.gray);
        activatedColor = GuiTools.getColorStateList(context, R.color.primary);
        color = activatedColor;
    }

    @Override
    public void activate(boolean start) {
        super.activate(start);
        animator.createAnimatorColor(getDrawable(), color.getDefaultColor(), activatedColor.getDefaultColor(), getResources().getInteger(R.integer.durationShort) * 2).start();
        color = activatedColor;
    }

    @Override
    public void deactivate(int reason) {
        super.deactivate(reason);
        animator.createAnimatorColor(getDrawable(), color.getDefaultColor(), deactivatedColor.getDefaultColor(), getResources().getInteger(R.integer.durationShort) * 2).start();
        color = deactivatedColor;
    }
}

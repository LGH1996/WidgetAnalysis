package com.lgh.widgetanalysis;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lgh.widgetanalysis.databinding.ViewMessageBinding;

public class MessageView extends FrameLayout implements View.OnClickListener {

    ViewMessageBinding viewMessageBinding;
    boolean onOff;

    public MessageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MessageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MessageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MessageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_message, this);
        TextView onOff = findViewById(R.id.on_off);
        TextView min = findViewById(R.id.min);
        TextView close = findViewById(R.id.close);
        Typeface iconFont = Typeface.createFromAsset(context.getAssets(), "iconfont.ttf");
        onOff.setTypeface(iconFont);
        min.setTypeface(iconFont);
        close.setTypeface(iconFont);
        onOff.setText(R.string.invisible);
        min.setText(R.string.min);
        close.setText(R.string.close);
        onOff.setOnClickListener(this);
        min.setOnClickListener(this);
        close.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.on_off:
                ((TextView) view).setText(onOff ? R.string.invisible : R.string.visible);
                onOff = !onOff;
                break;
            case R.id.min:

                break;
            case R.id.close:
                break;
        }
    }
}

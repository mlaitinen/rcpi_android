package fi.ninjaware.udpcarcontroller;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by miku on 2/6/14.
 */
public class ArrowImageView extends ImageView {

    private Paint paint;

    private float magnitude;

    private Bitmap arrow;

    private ArrowDrawer arrowDrawer;

    public ArrowImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.RED);

        arrow = ((BitmapDrawable) getDrawable()).getBitmap();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArrowImageView, 0, 0);
        switch(a.getInt(R.styleable.ArrowImageView_orientation, 0)) {
            case 0:
                arrowDrawer = new HorizontalArrowDrawer();
                break;
            case 1:
                arrowDrawer = new VerticalArrowDrawer();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        arrowDrawer.draw(canvas);
        canvas.drawBitmap(arrow, 0, 0, paint);
    }

    public void setMagnitude(int magnitude) {
        this.magnitude = (float) magnitude;
    }

    private interface ArrowDrawer {
        public void draw(Canvas canvas);
    }

    private class VerticalArrowDrawer implements ArrowDrawer {
        public void draw(Canvas canvas) {
            float center = getHeight()/2;
            float direction = center * (-magnitude / 100.0f) + center;
            if(magnitude < 0) {
                canvas.drawRect(0, center, 160, direction, paint);
            } else {
                canvas.drawRect(0, direction, 160, center, paint);
            }
        }
    }

    private class HorizontalArrowDrawer implements ArrowDrawer {
        public void draw(Canvas canvas) {
            float center = getWidth()/2;
            float direction = center * (-magnitude / 100.0f) + center;
            if(magnitude < 0) {
                canvas.drawRect(center, 0, direction, 160, paint);
            } else {
                canvas.drawRect(direction, 0, center, 160, paint);
            }
        }
    }
}

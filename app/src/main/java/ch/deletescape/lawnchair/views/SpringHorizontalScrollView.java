/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import ch.deletescape.lawnchair.util.ReflectionUtils;

public class SpringHorizontalScrollView extends HorizontalScrollView {
    private final SpringEdgeEffect.Manager springManager = new SpringEdgeEffect.Manager(this);

    {
        try {
            ReflectionUtils.getField(NestedScrollView.class.getName(), "mEdgeGlowLeft").set(this, springManager.createEdgeEffect(RecyclerView.EdgeEffectFactory.DIRECTION_RIGHT, true));
            ReflectionUtils.getField(NestedScrollView.class.getName(), "mEdgeGlowRight").set(this, springManager.createEdgeEffect(RecyclerView.EdgeEffectFactory.DIRECTION_RIGHT, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    }

    public SpringHorizontalScrollView(Context context) {
        super(context, null, 0);
    }

    public SpringHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SpringHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {
        springManager.withSpring(canvas, true, () -> {
            super.draw(canvas);
            return false;
        });
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        springManager.withSpring(canvas, false, () -> {
            super.dispatchDraw(canvas);
            return false;
        });
    }
}

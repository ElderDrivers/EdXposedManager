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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class SpringRecyclerView extends RecyclerView {

    private final SpringEdgeEffect.Manager springManager = new SpringEdgeEffect.Manager(this);

    {
        setEdgeEffectFactory(springManager.createFactory());
    }

    public SpringRecyclerView(@NonNull Context context) {
        this(context, null, 0);
    }

    public SpringRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpringRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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

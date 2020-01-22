// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.firebase.samples.apps.mlkit.common;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import asia.fredd.tools.creditcardutils.base.CardDateThru;
import asia.fredd.tools.creditcardutils.base.CardType;
import asia.fredd.tools.creditcardutils.type.AmericanExpress;
import asia.fredd.tools.creditcardutils.type.DiscoverCard;
import asia.fredd.tools.creditcardutils.type.MasterCard;
import asia.fredd.tools.creditcardutils.type.UnionPay;
import asia.fredd.tools.creditcardutils.type.VisaCard;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview). The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 *
 * <p>Supports scaling and mirroring of the graphics relative the camera's preview properties. The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 *
 * <p>Associated {@link Graphic} items should use the following methods to convert to view
 * coordinates for the graphics that are drawn:
 *
 * <ol>
 *   <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 *       supplied value from the preview scale to the view scale.
 *   <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
 *       coordinate from the preview's coordinate system to the view coordinate system.
 * </ol>
 */
public class GraphicOverlay extends View {
  private final Object lock = new Object();
  private int previewWidth;
  private float widthScaleFactor = 1.0f;
  private int previewHeight;
  private float heightScaleFactor = 1.0f;
  private int facing = CameraSource.CAMERA_FACING_BACK;
  private final List<Graphic> graphics = new ArrayList<>();

  /**
   * Base class for a custom graphics object to be rendered within the graphic overlay. Subclass
   * this and implement the {@link Graphic#draw(Canvas)} method to define the graphics element. Add
   * instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
   */
  public abstract static class Graphic {
    private GraphicOverlay overlay;

    public Graphic(GraphicOverlay overlay) {
      this.overlay = overlay;
    }

    /**
     * Draw the graphic on the supplied canvas. Drawing should use the following methods to convert
     * to view coordinates for the graphics that are drawn:
     *
     * <ol>
     *   <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
     *       supplied value from the preview scale to the view scale.
     *   <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
     *       coordinate from the preview's coordinate system to the view coordinate system.
     * </ol>
     *
     * @param canvas drawing canvas
     */
    public abstract void draw(Canvas canvas);

    /**
     * Adjusts a horizontal value of the supplied value from the preview scale to the view scale.
     */
    public float scaleX(float horizontal) {
      return horizontal * overlay.widthScaleFactor;
    }

    /** Adjusts a vertical value of the supplied value from the preview scale to the view scale. */
    public float scaleY(float vertical) {
      return vertical * overlay.heightScaleFactor;
    }

    /** Returns the application context of the app. */
    public Context getApplicationContext() {
      return overlay.getContext().getApplicationContext();
    }

    /**
     * Adjusts the x coordinate from the preview's coordinate system to the view coordinate system.
     */
    public float translateX(float x) {
      if (overlay.facing == CameraSource.CAMERA_FACING_FRONT) {
        return overlay.getWidth() - scaleX(x);
      } else {
        return scaleX(x);
      }
    }

    /**
     * Adjusts the y coordinate from the preview's coordinate system to the view coordinate system.
     */
    public float translateY(float y) {
      return scaleY(y);
    }

    public void postInvalidate() {
      overlay.postInvalidate();
    }
  }

  private AlertDialog alertDialog;
  private Handler handler;
  private boolean isReject;
  private CardType mCard;

  public GraphicOverlay(final Context context, AttributeSet attrs) {
      super(context, attrs);
      alertDialog = new AlertDialog.Builder(context)
              .setNegativeButton("離開", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      isReject = true;
                      LocalBroadcastManager.getInstance(context)
                              .sendBroadcast(
                                      new Intent("asia.fredd.tools.creditcardutils")
                              );
                  }
              })
              .setNeutralButton("重讀", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      isReject = false;
                      mCard = null;
                  }
              })
              .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      isReject = true;
                      LocalBroadcastManager.getInstance(context)
                              .sendBroadcast(
                                      new Intent("asia.fredd.tools.creditcardutils")
                                              .putExtra("card_number", mCard != null ? mCard.getCardNumber() : null)
                                              .putExtra("card_date", mCard != null ? mCard.getCardDateThru().getDate() : null)
                              );
                  }
              }).create();
      handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
          @Override
          public boolean handleMessage(Message msg) {
              if (!isReject && alertDialog != null && !alertDialog.isShowing()) {
                  mCard = (CardType) msg.obj;
                  if (mCard instanceof AmericanExpress) {
                      alertDialog.setTitle("美國運通");
                  } else if (mCard instanceof DiscoverCard) {
                      alertDialog.setTitle("發現卡");
                  } else if (mCard instanceof VisaCard) {
                      alertDialog.setTitle("VISA");
                  } else if (mCard instanceof MasterCard) {
                      alertDialog.setTitle("MasterCard");
                  } else if (mCard instanceof UnionPay) {
                      alertDialog.setTitle("中國銀聯");
                  }
                  StringBuilder sb = new StringBuilder("卡號\n");
                  CharSequence cardNumber = mCard.getCardNumber();
                  switch (cardNumber.length()) {
                      case 15:
                          sb.append(cardNumber, 0, 4)
                                  .append(" ")
                                  .append(cardNumber, 4, 10)
                                  .append(" ")
                                  .append(cardNumber, 10, 15);
                          break;
                      case 16:
                          sb.append(cardNumber, 0, 4)
                                  .append(" ")
                                  .append(cardNumber, 4, 8)
                                  .append(" ")
                                  .append(cardNumber, 8, 12)
                                  .append(" ")
                                  .append(cardNumber, 12, 16);
                          break;
                      default:
                          sb.append(cardNumber);
                          break;
                  }
                  CardDateThru dateThru = mCard.getCardDateThru();
                  if (dateThru != null) {
                      CharSequence date = dateThru.getDate();
                      sb.append("\n\n有效期限\n")
                              .append(date, 0, 2)
                              .append("/")
                              .append(date, 2, 4);
                  }
                  alertDialog.setMessage(sb);
                  alertDialog.show();
              }
              return true;
          }
      });
  }

  /** Removes all graphics from the overlay. */
  public void clear() {
    synchronized (lock) {
      graphics.clear();
    }
    postInvalidate();
  }

  /** Adds a graphic to the overlay. */
  public void add(Graphic graphic) {
    synchronized (lock) {
      graphics.add(graphic);
    }
  }

  public void post(@NonNull CardType card) {
      Message.obtain(handler, 1, card).sendToTarget();
  }

  /** Removes a graphic from the overlay. */
  public void remove(Graphic graphic) {
    synchronized (lock) {
      graphics.remove(graphic);
    }
    postInvalidate();
  }

  /**
   * Sets the camera attributes for size and facing direction, which informs how to transform image
   * coordinates later.
   */
  public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
    synchronized (lock) {
      this.previewWidth = previewWidth;
      this.previewHeight = previewHeight;
      this.facing = facing;
    }
    postInvalidate();
  }

  /** Draws the overlay with its associated graphic objects. */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    synchronized (lock) {
      if ((previewWidth != 0) && (previewHeight != 0)) {
        widthScaleFactor = (float) getWidth() / previewWidth;
        heightScaleFactor = (float) getHeight() / previewHeight;
      }

      for (Graphic graphic : graphics) {
        graphic.draw(canvas);
      }
    }
  }
}

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
package com.google.firebase.samples.apps.mlkit.java.textrecognition;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import asia.fredd.tools.creditcardutils.base.CardDateThru;
import asia.fredd.tools.creditcardutils.base.CardType;
import asia.fredd.tools.creditcardutils.base.CreditCard;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.samples.apps.mlkit.common.FrameMetadata;
import com.google.firebase.samples.apps.mlkit.common.GraphicOverlay;
import com.google.firebase.samples.apps.mlkit.java.VisionProcessorBase;

import java.io.IOException;
import java.util.List;

/**
 * Processor for the text recognition demo.
 */
public class TextRecognitionProcessor extends VisionProcessorBase<FirebaseVisionText> {

    private static final String TAG = "TextRecProc";

    private final FirebaseVisionTextRecognizer detector;

    public TextRecognitionProcessor() {
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
        }
    }

    @Override
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull FirebaseVisionText results,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
//        if (originalCameraImage != null) {
//            com.google.firebase.samples.apps.mlkit.common.CameraImageGraphic imageGraphic;
//            imageGraphic = new com.google.firebase.samples.apps.mlkit.common.CameraImageGraphic(
//                    graphicOverlay,
//                    originalCameraImage
//            );
//            graphicOverlay.add(imageGraphic);
//        }
        List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();
        int count = blocks.size();
        FirebaseVisionText.TextBlock item;
        String text;
        CardType card = null;
        CardDateThru dateThru = null;
        for (int i = 0; i < count; ++i) {
            if ((item = blocks.get(i)) != null && (text = item.getText()).length() > 0) {
                if (card == null) {
                    card = CreditCard.ExtractCardNumber(text);
                    CharSequence o = CreditCard.ExtractNumber(text, CreditCard.DefaultPattern);
                    if (o != null) {
                        TextGraphic ocrGraphic = new TextGraphic(graphicOverlay, item);
                        graphicOverlay.add(ocrGraphic);
                    }
                }
                if (dateThru == null) {
                    dateThru = CreditCard.ExtractCardDateThru(text);
                    if (dateThru != null) {
                        TextGraphic ocrGraphic = new TextGraphic(graphicOverlay, item);
                        graphicOverlay.add(ocrGraphic);
                    }
                }
            }
            if (card != null && dateThru != null) {
                Log.d("OcrDetectorProcessor", "Card Number detected! = " + card.getCardNumber());
                Log.d("OcrDetectorProcessor", "Card Date Thru detected! = " + dateThru.getDate());
                card.setCardDateThru(dateThru);
                graphicOverlay.post(card);
                break;
            }
        }
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
    }
}

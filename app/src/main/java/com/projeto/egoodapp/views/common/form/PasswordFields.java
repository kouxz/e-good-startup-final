package com.projeto.egoodapp.views.common.form;

import android.text.GetChars;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Checkable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class PasswordFields {
    private PasswordFields() {
    }

    public static void configure(TextInputLayout layout, TextInputEditText field) {
        field.setTransformationMethod(NoPreviewTransformationMethod.getInstance());
        layout.setEndIconCheckable(true);
        layout.setEndIconOnClickListener(view -> {
            boolean wasHidden = field.getTransformationMethod()
                    instanceof PasswordTransformationMethod;
            field.setTransformationMethod(wasHidden
                    ? null : NoPreviewTransformationMethod.getInstance());
            if (view instanceof Checkable) ((Checkable) view).setChecked(wasHidden);
            field.setSelection(field.length());
        });
    }

    private static final class NoPreviewTransformationMethod
            extends PasswordTransformationMethod {
        private static final char MASK = '\u2022';
        private static final NoPreviewTransformationMethod INSTANCE =
                new NoPreviewTransformationMethod();

        public static NoPreviewTransformationMethod getInstance() {
            return INSTANCE;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source == null ? null : new MaskedSequence(source);
        }

        @Override
        public void onTextChanged(CharSequence source, int start, int before, int count) {
            // Avoid Android's temporary preview of the last typed character.
        }

        private static final class MaskedSequence implements CharSequence, GetChars {
            private final CharSequence source;

            MaskedSequence(CharSequence source) {
                this.source = source;
            }

            @Override
            public int length() {
                return source.length();
            }

            @Override
            public char charAt(int index) {
                if (index < 0 || index >= source.length()) throw new IndexOutOfBoundsException();
                return MASK;
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                if (start < 0 || end > source.length() || start > end) {
                    throw new IndexOutOfBoundsException();
                }
                return maskedText(end - start);
            }

            @Override
            public void getChars(int start, int end, char[] destination, int destinationOffset) {
                if (start < 0 || end > source.length() || start > end) {
                    throw new IndexOutOfBoundsException();
                }
                for (int index = start; index < end; index++) {
                    destination[destinationOffset + index - start] = MASK;
                }
            }

            @Override
            public String toString() {
                return maskedText(source.length());
            }

            private static String maskedText(int length) {
                StringBuilder result = new StringBuilder(length);
                for (int index = 0; index < length; index++) result.append(MASK);
                return result.toString();
            }
        }
    }
}

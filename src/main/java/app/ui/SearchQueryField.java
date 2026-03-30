package app.ui;

import javax.swing.JTextField;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputMethodEvent;
import java.text.AttributedCharacterIterator;

public final class SearchQueryField extends JTextField {

    private final CompositionAwareCaret compositionCaret = new CompositionAwareCaret();
    private boolean imeCompositionActive;

    public SearchQueryField() {
        super();
        setColumns(1);
        setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        super.setCaret(compositionCaret);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                updateImeCompositionState(false);
                syncManagedCaret();
            }
        });
    }

    @Override
    protected void processInputMethodEvent(InputMethodEvent event) {
        super.processInputMethodEvent(event);

        if (event.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            updateImeCompositionState(hasComposedText(event));
            syncManagedCaret();
        } else if (event.getID() == InputMethodEvent.CARET_POSITION_CHANGED && imeCompositionActive) {
            syncManagedCaret();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        preferred.height = Math.max(preferred.height, 36);
        return preferred;
    }

    boolean isImeCompositionActive() {
        return imeCompositionActive;
    }

    boolean isManagedCaretInstalled() {
        return getCaret() == compositionCaret;
    }

    private void updateImeCompositionState(boolean active) {
        if (imeCompositionActive == active) {
            return;
        }
        imeCompositionActive = active;
    }

    private void syncManagedCaret() {
        Caret activeCaret = getCaret();
        if (activeCaret != compositionCaret) {
            int dot = activeCaret == null ? 0 : activeCaret.getDot();
            int mark = activeCaret == null ? dot : activeCaret.getMark();
            int blinkRate = activeCaret == null ? compositionCaret.getBlinkRate() : activeCaret.getBlinkRate();

            super.setCaret(compositionCaret);
            compositionCaret.setBlinkRate(blinkRate);
            restoreCaretSelection(dot, mark);
        }

        compositionCaret.setImeCompositionActive(imeCompositionActive);
        compositionCaret.setVisible(!imeCompositionActive && hasFocus());
    }

    private void restoreCaretSelection(int dot, int mark) {
        int length = getDocument().getLength();
        int clampedDot = Math.max(0, Math.min(length, dot));
        int clampedMark = Math.max(0, Math.min(length, mark));

        compositionCaret.setDot(clampedMark);
        if (clampedDot != clampedMark) {
            compositionCaret.moveDot(clampedDot);
        } else {
            compositionCaret.setDot(clampedDot);
        }
    }

    private static boolean hasComposedText(InputMethodEvent event) {
        AttributedCharacterIterator text = event.getText();
        if (text == null) {
            return false;
        }
        int totalCharacters = text.getEndIndex() - text.getBeginIndex();
        return totalCharacters > event.getCommittedCharacterCount();
    }

    private static final class CompositionAwareCaret extends DefaultCaret {

        private boolean imeCompositionActive;

        private void setImeCompositionActive(boolean active) {
            if (imeCompositionActive == active) {
                return;
            }

            imeCompositionActive = active;
            super.setVisible(active ? false : isActive());
            repaint();
        }

        @Override
        public void paint(Graphics graphics) {
            if (!imeCompositionActive) {
                super.paint(graphics);
            }
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(imeCompositionActive ? false : visible);
        }
    }
}

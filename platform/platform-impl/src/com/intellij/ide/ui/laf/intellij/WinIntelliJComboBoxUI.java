/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.ui.laf.intellij;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeListener;

/**
 * @author Konstantin Bulenkov
 */
public class WinIntelliJComboBoxUI extends DarculaComboBoxUI {
  private static final String HOVER_PROPERTY = "JComboBox.mouseHover";
  private static final String PRESSED_PROPERTY = "JComboBox.mousePressed";
  private static final Border DEFAULT_EDITOR_BORDER = JBUI.Borders.empty(1, 0);
  private static final Dimension ARROW_BUTTON_SIZE = new JBDimension(21, 24); // Count borders

  private MouseListener mouseListener;

  private MouseListener buttonReleaseListener;
  private MouseListener buttonHoverListener;
  private PropertyChangeListener propertyListener;

  private MouseListener editorHoverListener;
  private KeyListener   editorKeyListener;
  private FocusListener editorFocusListener;

  public WinIntelliJComboBoxUI(JComboBox comboBox) {
    super(comboBox);
  }

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new WinIntelliJComboBoxUI((JComboBox)c);
  }

  @Override protected void installListeners() {
    super.installListeners();

    if (!comboBox.isEditable()) {
      comboBox.addMouseListener(mouseListener = new ComboBoxMouseListener());
    }

    propertyListener = (evt) -> {
      if("enabled".equals(evt.getPropertyName())) {
        EditorTextField etf = UIUtil.findComponentOfType((JComponent)editor, EditorTextField.class);
        if (etf != null) {
          etf.setBackground(getComboBackground(true));
        }
      } else if ("editable".equals(evt.getPropertyName())) {
        if (evt.getNewValue() == Boolean.TRUE) {
          comboBox.removeMouseListener(mouseListener);
        } else {
          comboBox.addMouseListener(mouseListener);
        }
      }
    };

    comboBox.addPropertyChangeListener(propertyListener);
  }

  @Override public void uninstallListeners() {
    super.uninstallListeners();
    comboBox.removeMouseListener(mouseListener);

    if (propertyListener != null) {
      comboBox.removePropertyChangeListener(propertyListener);
      propertyListener = null;
    }
  }

  @Override public void paint(Graphics g, JComponent c) {
    Graphics2D g2 = (Graphics2D)g.create();
    try{
      Rectangle r = new Rectangle(c.getSize());

      if (c.isOpaque()) {
        Container parent = c.getParent();
        if (parent != null) {
          g2.setColor(isTableCellEditor(c) && editor != null ? editor.getBackground() : parent.getBackground());
          g2.fill(r);
        }
      }

      g2.setColor(getComboBackground(editor != null && editor.isOpaque()));
      JBInsets.removeFrom(r, JBUI.insets(2));

      if (!comboBox.isEnabled()) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.47f));
      }

      g2.fill(r);

      if (!comboBox.isEditable()) {
        hasFocus = comboBox.hasFocus();
        paintCurrentValue(g2, rectangleForCurrentValue(), hasFocus);
      }
    } finally {
      g2.dispose();
    }
  }

  @Override protected Rectangle rectangleForCurrentValue() {
    int w = comboBox.getWidth();
    int h = comboBox.getHeight();
    Insets i = comboBox.getInsets();

    int buttonWidth = h;
    if (arrowButton != null) {
      buttonWidth = comboBox.getComponentOrientation().isLeftToRight() ?
                    arrowButton.getWidth() - i.right: arrowButton.getWidth() - i.left;
    }

    Rectangle rect = (comboBox.getComponentOrientation().isLeftToRight()) ?
      new Rectangle(i.left, i.top,
                           w - (i.left + i.right + buttonWidth),
                           h - (i.top + i.bottom)) :
      new Rectangle(i.left + buttonWidth, i.top,
                           w - (i.left + i.right + buttonWidth),
                           h - (i.top + i.bottom));

    if (editor instanceof JComponent) {
      JBInsets.removeFrom(rect, ((JComponent)editor).getInsets());
    }

    return rect;
  }


  public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
    @SuppressWarnings("unchecked")
    ListCellRenderer<Object> renderer = comboBox.getRenderer();
    Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);

    c.setFont(comboBox.getFont());
    c.setForeground(comboBox.isEnabled() ? UIManager.getColor("Label.foreground") : UIManager.getColor("Label.disabledForeground"));

    if (c instanceof JComponent) {
      ((JComponent)c).setBorder(DEFAULT_EDITOR_BORDER);
    }

    // paint selection in table-cell-editor mode correctly
    boolean changeOpaque = c instanceof JComponent && c.isOpaque();
    if (changeOpaque) {
      ((JComponent)c).setOpaque(false);
    }

    Rectangle r = new Rectangle(bounds);
    Insets i = UIManager.getInsets("ComboBox.padding");
    if (i == null && c instanceof JComponent) {
      i = ((JComponent)c).getInsets();
    }

    JBInsets.removeFrom(r, i);

    currentValuePane.paintComponent(g, c, comboBox, r.x, r.y, r.width, r.height, c instanceof JPanel);

    // return opaque for combobox popup items painting
    if (changeOpaque) {
      ((JComponent)c).setOpaque(true);
    }
  }

  private Color getComboBackground(boolean opaque) {
    if (comboBox != null) {
      if (comboBox.isEnabled() && comboBox.isEditable()) {
        return UIManager.getColor("TextField.background");
      } else if (!comboBox.isEnabled()) {
        return opaque ? UIManager.getColor("Button.background.opaque") : UIManager.getColor("Button.background");
      } else if (!comboBox.isEditable()) {
        if (isPressed()) {
          return UIManager.getColor("Button.intellij.native.pressedBackgroundColor");
        } else if (isHover()) {
          return UIManager.getColor("Button.intellij.native.focusedBackgroundColor");
        }
      }
    }
    return UIManager.getColor("Button.background");
  }

  @Override
  protected JButton createArrowButton() {
    JButton button = new BasicArrowButton(SwingConstants.SOUTH) {
      @Override
      public Dimension getPreferredSize() {
        return ARROW_BUTTON_SIZE;
      }

      @Override
      public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        try {
          Rectangle innerRect = new Rectangle(getSize());
          JBInsets.removeFrom(innerRect, getInsets());

          // paint background
          if (comboBox.isEditable() && comboBox.isEnabled()) {
            if (isPressed()) {
              g2.setColor(UIManager.getColor("Button.intellij.native.pressedBackgroundColor"));
            } else if (comboBox.hasFocus() || isHover()) {
              g2.setColor(UIManager.getColor("Button.intellij.native.focusedBackgroundColor"));
            } else {
              g2.setColor(getComboBackground(false));
            }
            g2.fill(innerRect);
          }

          // paint border around button when combobox is editable
          if (comboBox.isEditable() && comboBox.isEnabled()) {
            Path2D border = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            Rectangle outerRect = new Rectangle(innerRect);
            JBInsets.addTo(outerRect, JBUI.insets(1));
            border.append(outerRect, false);
            border.append(innerRect, false);

            if (getModel().isPressed()) {
              g2.setColor(UIManager.getColor("Button.intellij.native.pressedBorderColor"));
              g2.fill(border);
            } else if (comboBox.hasFocus() || isHover()) {
              g2.setColor(UIManager.getColor("Button.intellij.native.focusedBorderColor"));
              g2.fill(border);
            }
          }

          if (!comboBox.isEnabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
          }

          Icon icon = MacIntelliJIconCache.getIcon("comboDropTriangle", false, false, isEnabled());
          int x = JBUI.scale(5);
          int y = (getHeight() - icon.getIconHeight()) / 2;
          icon.paintIcon(this, g2, x, y);

        } finally {
          g2.dispose();
        }
      }
    };

    button.setOpaque(false);

    button.setBorder(comboBox.getComponentOrientation().isLeftToRight() ?
                     JBUI.Borders.empty(2, 1, 2, 2) : JBUI.Borders.empty(2, 2, 2, 1));
    buttonReleaseListener = new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        if (!comboBox.isEditable()) {
          comboBox.repaint();
        }
      }
    };

    buttonHoverListener = new DarculaUIUtil.MouseHoverPropertyTrigger(comboBox, HOVER_PROPERTY);

    button.addMouseListener(buttonHoverListener);
    button.addMouseListener(buttonReleaseListener);
    return button;
  }

  @Override public void unconfigureArrowButton() {
    super.unconfigureArrowButton();
    if (arrowButton != null) {
      arrowButton.removeMouseListener(buttonReleaseListener);
      arrowButton.removeMouseListener(buttonHoverListener);
    }
  }


  @Override
  protected ComboBoxEditor createEditor() {
    ComboBoxEditor comboBoxEditor = new BasicComboBoxEditor.UIResource() {
      @Override
      protected JTextField createEditorComponent() {
        return new JTextField() {
          public void setText(String s) {
            if (getText().equals(s)) {
              return;
            }
            super.setText(s);
          }

          @Override public Color getBackground() {
            return getComboBackground(false);
          }

          @Override public Border getBorder() {
            return DEFAULT_EDITOR_BORDER;
          }

          @Override public Insets getInsets() {
            return DEFAULT_EDITOR_BORDER.getBorderInsets(this);
          }

          @Override
          public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, Math.max(JBUI.scale(18), size.height));
          }
        };
      }
    };

    Component ec = comboBoxEditor.getEditorComponent();
    if (ec != null) {
      editorKeyListener = new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
          process(e);
        }
        @Override public void keyReleased(KeyEvent e) {
          process(e);
        }

        private void process(KeyEvent e) {
          int code = e.getKeyCode();
          if ((code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) && e.getModifiers() == 0) {
            comboBox.dispatchEvent(e);
          }
        }
      };

      ec.addKeyListener(editorKeyListener);
    }
    return comboBoxEditor;
  }

  @Override protected void configureEditor() {
    super.configureEditor();

    if (editor != null) {
      editorFocusListener = new FocusAdapter() {
        @Override public void focusGained(FocusEvent e) {
          update();
        }
        @Override public void focusLost(FocusEvent e) {
          update();
        }

        private void update() {
          if (comboBox != null) {
            comboBox.repaint();
          }
        }
      };

      editorHoverListener = new DarculaUIUtil.MouseHoverPropertyTrigger(comboBox, HOVER_PROPERTY);

      if (editor instanceof JTextComponent) {
        editor.addFocusListener(editorFocusListener);
        editor.addMouseListener(editorHoverListener);
      } else {
        EditorTextField etf = UIUtil.findComponentOfType((JComponent)editor, EditorTextField.class);
        if (etf != null) {
          etf.addFocusListener(editorFocusListener);
          etf.addMouseListener(editorHoverListener);
          etf.setBackground(getComboBackground(true));
        }
      }

      JComponent jc = (JComponent)editor;
      jc.setBorder(DEFAULT_EDITOR_BORDER);
      jc.setOpaque(false);
    }
  }

  @Override protected void unconfigureEditor() {
    super.unconfigureEditor();

    if (editorKeyListener != null) {
      editor.removeKeyListener(editorKeyListener);
    }

    if (editor instanceof JTextComponent) {
      if (editorFocusListener != null) {
        editor.removeFocusListener(editorFocusListener);
      }

      if (editorHoverListener != null) {
        editor.removeMouseListener(editorHoverListener);
      }
    } else {
      EditorTextField etf = UIUtil.findComponentOfType((JComponent)editor, EditorTextField.class);
      if (etf != null) {
        if (editorHoverListener != null) {
          etf.removeFocusListener(editorFocusListener);
        }

        if (editorHoverListener != null) {
          etf.removeMouseListener(editorHoverListener);
        }
      }
    }
  }

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    if (comboBox == null || arrowButton == null) {
      return; //NPE on LaF change
    }

    Graphics2D g2 = (Graphics2D)g.create();

    try {
      g2.translate(x, y);

      checkFocus();
      if (Registry.is("ide.inplace.errors.outline") && comboBox.getClientProperty("JComponent.error.outline") == Boolean.TRUE) {
        DarculaUIUtil.paintErrorBorder(g2, width, height, 0, true, hasFocus);
      } else if (comboBox.isEnabled()) {
        if (comboBox.isEditable()) {
          if (hasFocus) {
            g2.setColor(UIManager.getColor("TextField.focusedBorderColor"));
          } else {
            g2.setColor(UIManager.getColor(isEditorHover() ? "TextField.hoverBorderColor" : "TextField.borderColor"));
          }
        } else {
          if (isPressed()) {
            g2.setColor(UIManager.getColor("Button.intellij.native.pressedBorderColor"));
          } else if (isHover() || hasFocus) {
            g2.setColor(UIManager.getColor("Button.intellij.native.focusedBorderColor"));
          } else {
            g2.setColor(UIManager.getColor("Button.intellij.native.borderColor"));
          }
        }
      } else {
        g2.setColor(UIManager.getColor("Button.intellij.native.borderColor"));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.47f));
      }

      Path2D border = new Path2D.Double(Path2D.WIND_EVEN_ODD);
      Rectangle outerRect = new Rectangle(width, height);
      JBInsets.removeFrom(outerRect, JBUI.insets(1));
      border.append(outerRect, false);

      Rectangle innerRect = new Rectangle(outerRect);
      JBInsets.removeFrom(innerRect, JBUI.insets(1));
      border.append(innerRect, false);
      g2.fill(border);
    } finally {
      g2.dispose();
    }
  }

  private boolean isHover() {
    return comboBox != null && comboBox.getClientProperty(HOVER_PROPERTY) == Boolean.TRUE ||
           arrowButton != null && arrowButton.getClientProperty(HOVER_PROPERTY) == Boolean.TRUE;
  }

  private boolean isEditorHover() {
    JComponent ec = (JComponent)comboBox.getEditor().getEditorComponent();
    EditorTextField etf = UIUtil.findComponentOfType((JComponent)editor, EditorTextField.class);
    Editor editor = etf != null ? etf.getEditor() : null;

    return arrowButton != null && arrowButton.getClientProperty(HOVER_PROPERTY) == Boolean.TRUE ||
           ec != null && ec.getClientProperty(HOVER_PROPERTY) == Boolean.TRUE ||
           editor != null && editor.getContentComponent().getClientProperty(HOVER_PROPERTY) == Boolean.TRUE;
  }

  private boolean isPressed() {
    return comboBox != null && comboBox.getClientProperty(PRESSED_PROPERTY) == Boolean.TRUE ||
           arrowButton != null && arrowButton.getModel().isPressed();
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return c.getComponentOrientation().isLeftToRight() ?
           JBUI.insets(2, 6, 2, 2).asUIResource() : JBUI.insets(2, 2, 2, 6).asUIResource();
  }

  private Dimension getSizeWithButton(Dimension d) {
    Insets i = comboBox.getInsets();
    int width = ARROW_BUTTON_SIZE.width + i.left;
    return new Dimension(Math.max(d.width + JBUI.scale(10), width), ARROW_BUTTON_SIZE.height);
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    return getSizeWithButton(super.getPreferredSize(c));
  }

  @Override
  public Dimension getMinimumSize(JComponent c) {
    return getSizeWithButton(super.getMinimumSize(c));
  }

  @Override
  protected LayoutManager createLayoutManager() {
    return new ComboBoxLayoutManager() {
      @Override
      public void layoutContainer(Container parent) {
        JComboBox cb = (JComboBox)parent;

        int buttonHeight = cb.getHeight();
        int buttonWidth = ARROW_BUTTON_SIZE.width;

        if (arrowButton != null) {
          if (cb.getComponentOrientation().isLeftToRight()) {
            arrowButton.setBounds(cb.getWidth() - buttonWidth, 0, buttonWidth, buttonHeight);
          } else {
            arrowButton.setBounds(0, 0, buttonWidth, buttonHeight);
          }
        }

        if (editor != null) {
          Rectangle er = rectangleForCurrentValue();
          editor.setBounds(er);
        }
      }
    };
  }

  private class ComboBoxMouseListener extends MouseAdapter {
    @Override public void mousePressed(MouseEvent e) {
      setPressedProperty(true);
    }

    @Override public void mouseReleased(MouseEvent e) {
      setPressedProperty(false);
    }

    private void setPressedProperty(boolean isPressed) {
      if (!comboBox.isEditable()) {
        comboBox.putClientProperty(PRESSED_PROPERTY, Boolean.valueOf(isPressed));
        comboBox.repaint();
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      setHoverProperty(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setHoverProperty(false);
    }

    private void setHoverProperty(boolean isHover) {
      comboBox.putClientProperty(HOVER_PROPERTY, Boolean.valueOf(isHover));
      comboBox.repaint();
    }
  }
}

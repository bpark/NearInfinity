// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.ScriptTextArea;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.DialogSearcher;
import org.infinity.util.StringResource;

final class Viewer extends JPanel implements ActionListener, ItemListener, TableModelListener
{
  private static final ButtonPanel.Control CtrlNextState      = ButtonPanel.Control.CUSTOM_1;
  private static final ButtonPanel.Control CtrlPrevState      = ButtonPanel.Control.CUSTOM_2;
  private static final ButtonPanel.Control CtrlNextTrans      = ButtonPanel.Control.CUSTOM_3;
  private static final ButtonPanel.Control CtrlPrevTrans      = ButtonPanel.Control.CUSTOM_4;
  private static final ButtonPanel.Control CtrlSelect         = ButtonPanel.Control.CUSTOM_5;
  private static final ButtonPanel.Control CtrlUndo           = ButtonPanel.Control.CUSTOM_6;
  private static final ButtonPanel.Control CtrlStateField     = ButtonPanel.Control.CUSTOM_7;
  private static final ButtonPanel.Control CtrlResponseField  = ButtonPanel.Control.CUSTOM_8;

  private final ButtonPanel buttonPanel = new ButtonPanel();
  private final DlgPanel stateTextPanel, stateTriggerPanel, transTextPanel, transTriggerPanel, transActionPanel;
  private final DlgResource dlg;
  private final JMenuItem ifindall = new JMenuItem("in all DLG files");
  private final JMenuItem ifindthis = new JMenuItem("in this file only");
  private final JPanel outerpanel;
  private final JTextField tfState = new JTextField(4);
  private final JTextField tfResponse = new JTextField(4);
  private final List<Action> actionList = new ArrayList<Action>();
  private final List<ResponseTrigger> transTriList = new ArrayList<ResponseTrigger>();
  private final List<State> stateList = new ArrayList<State>();
  private final List<StateTrigger> staTriList = new ArrayList<StateTrigger>();
  private final List<Transition> transList = new ArrayList<Transition>();
  private final Stack<State> lastStates = new Stack<State>();
  private final Stack<Transition> lastTransitions = new Stack<Transition>();
  private final TitledBorder bostate = new TitledBorder("State");
  private final TitledBorder botrans = new TitledBorder("Response");
  private State currentstate;
  private Transition currenttransition;
  private boolean alive = true;
  private DlgResource undoDlg;

  Viewer(DlgResource dlg)
  {
    this.dlg = dlg;
    this.dlg.addTableModelListener(this);

    ButtonPopupMenu bpmFind = (ButtonPopupMenu)ButtonPanel.createControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis});
    bpmFind.addItemListener(this);
    bpmFind.addActionListener(this);

    JButton bNextState = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextState.setMargin(new Insets(bNextState.getMargin().top, 0, bNextState.getMargin().bottom, 0));
    bNextState.addActionListener(this);

    JButton bPrevState = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevState.setMargin(bNextState.getMargin());
    bPrevState.addActionListener(this);

    JButton bNextTrans = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextTrans.setMargin(bNextState.getMargin());
    bNextTrans.addActionListener(this);

    JButton bPrevTrans = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevTrans.setMargin(bNextState.getMargin());
    bPrevTrans.addActionListener(this);

    JButton bSelect = new JButton("Select", Icons.getIcon(Icons.ICON_REDO_16));
    bSelect.addActionListener(this);

    JButton bUndo = new JButton("Undo", Icons.getIcon(Icons.ICON_UNDO_16));
    bUndo.addActionListener(this);

    int width = (int)tfState.getPreferredSize().getWidth();
    int height = (int)bNextState.getPreferredSize().getHeight();
    tfState.setPreferredSize(new Dimension(width, height));
    tfResponse.setPreferredSize(new Dimension(width, height));
    tfState.setHorizontalAlignment(JTextField.CENTER);
    tfResponse.setHorizontalAlignment(JTextField.CENTER);
    tfState.addActionListener(this);
    tfResponse.addActionListener(this);
    stateTextPanel = new DlgPanel("Text", true);
    stateTriggerPanel = new DlgPanel("Trigger", false, true);
    transTextPanel = new DlgPanel("Text", true);
    transTriggerPanel = new DlgPanel("Trigger", false, true);
    transActionPanel = new DlgPanel("Action", false, true);

    JPanel statepanel = new JPanel();
    statepanel.setLayout(new GridLayout(2, 1, 6, 6));
    statepanel.add(stateTextPanel);
    statepanel.add(stateTriggerPanel);
    statepanel.setBorder(bostate);

    JPanel transpanel2 = new JPanel();
    transpanel2.setLayout(new GridLayout(1, 2, 6, 6));
    transpanel2.add(transTriggerPanel);
    transpanel2.add(transActionPanel);
    JPanel transpanel = new JPanel();
    transpanel.setLayout(new GridLayout(2, 1, 6, 6));
    transpanel.add(transTextPanel);
    transpanel.add(transpanel2);
    transpanel.setBorder(botrans);

    outerpanel = new JPanel();
    outerpanel.setLayout(new GridLayout(2, 1, 6, 6));
    outerpanel.add(statepanel);
    outerpanel.add(transpanel);

    buttonPanel.addControl(new JLabel("State:"));
    buttonPanel.addControl(tfState, CtrlStateField);
    buttonPanel.addControl(bPrevState, CtrlPrevState);
    buttonPanel.addControl(bNextState, CtrlNextState);
    buttonPanel.addControl(new JLabel(" Response:"));
    buttonPanel.addControl(tfResponse, CtrlResponseField);
    buttonPanel.addControl(bPrevTrans, CtrlPrevTrans);
    buttonPanel.addControl(bNextTrans, CtrlNextTrans);
    buttonPanel.addControl(bSelect, CtrlSelect);
    buttonPanel.addControl(bUndo, CtrlUndo);
    buttonPanel.addControl(bpmFind, ButtonPanel.Control.FIND_MENU);

    setLayout(new BorderLayout());
    add(outerpanel, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
    outerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    updateViewerLists();

    if (stateList.size() > 0) {
      showState(0);
      showTransition(currentstate.getFirstTrans());
    } else {
      bPrevState.setEnabled(false);
      bNextState.setEnabled(false);
      bPrevTrans.setEnabled(false);
      bNextTrans.setEnabled(false);
      bSelect.setEnabled(false);
    }
    bUndo.setEnabled(false);
  }

  public void setUndoDlg(DlgResource dlg)
  {
    this.undoDlg = dlg;
    buttonPanel.getControlByType(CtrlUndo).setEnabled(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (!alive) return;
    if (buttonPanel.getControlByType(CtrlUndo) == event.getSource()) {
      JButton bUndo = (JButton)event.getSource();
      if(lastStates.empty() && (undoDlg != null)) {
        showExternState(undoDlg, -1, true);
        return;
      }
      State oldstate = lastStates.pop();
      Transition oldtrans = lastTransitions.pop();
      if (lastStates.empty() && (undoDlg == null)) {
        bUndo.setEnabled(false);
      }
      if (oldstate != currentstate) {
        showState(oldstate.getNumber());
      }
      if (oldtrans != currenttransition) {
        showTransition(oldtrans.getNumber());
      }
    } else {
      int newstate = currentstate.getNumber();
      int newtrans = currenttransition.getNumber();
      if (buttonPanel.getControlByType(CtrlNextState) == event.getSource()) {
        newstate++;
      } else if (buttonPanel.getControlByType(CtrlPrevState) == event.getSource()) {
        newstate--;
      } else if (buttonPanel.getControlByType(CtrlNextTrans) == event.getSource()) {
        newtrans++;
      } else if (buttonPanel.getControlByType(CtrlPrevTrans) == event.getSource()) {
        newtrans--;
      } else if (event.getSource() == tfState) {
        try {
          int number = Integer.parseInt(tfState.getText());
          if (number >= 0 && number <= stateList.size()) {
            newstate = number;
          } else {
            tfState.setText(String.valueOf(currentstate.getNumber()));
          }
        } catch (Exception e) {
          tfState.setText(String.valueOf(currentstate.getNumber()));
        }
      } else if (event.getSource() == tfResponse) {
        try {
          int number = Integer.parseInt(tfResponse.getText());
          if (number >= 0 && number <= currentstate.getTransCount()) {
            newtrans = currentstate.getFirstTrans() + number;
          } else {
            tfResponse.setText(String.valueOf(currenttransition.getNumber() - currentstate.getFirstTrans()));
          }
        } catch (Exception e) {
          tfResponse.setText(String.valueOf(currenttransition.getNumber() - currentstate.getFirstTrans()));
        }
      } else if (buttonPanel.getControlByType(CtrlSelect) == event.getSource()) {
        ResourceRef next_dlg = currenttransition.getNextDialog();
        if (dlg.getResourceEntry().toString().equalsIgnoreCase(next_dlg.toString())) {
          lastStates.push(currentstate);
          lastTransitions.push(currenttransition);
          buttonPanel.getControlByType(CtrlUndo).setEnabled(true);
          newstate = currenttransition.getNextDialogState();
        } else {
          DlgResource newdlg =
              (DlgResource)ResourceFactory.getResource(ResourceFactory.getResourceEntry(next_dlg.toString()));
          showExternState(newdlg, currenttransition.getNextDialogState(), false);
        }
      }
      if (alive) {
        if (newstate != currentstate.getNumber()) {
          showState(newstate);
          showTransition(stateList.get(newstate).getFirstTrans());
        } else if (newtrans != currenttransition.getNumber()) {
          showTransition(newtrans);
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU) == event.getSource()) {
      ButtonPopupMenu bpmFind = (ButtonPopupMenu)event.getSource();
      if (bpmFind.getSelectedItem() == ifindall) {
        List<ResourceEntry> files = ResourceFactory.getResources("DLG");
        new DialogSearcher(files, getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<ResourceEntry>();
        files.add(dlg.getResourceEntry());
        new DialogSearcher(files, getTopLevelAncestor());
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent e)
  {
    updateViewerLists();
    showState(currentstate.getNumber());
    showTransition(currenttransition.getNumber());
  }

// --------------------- End Interface TableModelListener ---------------------

  // for quickly jump to the corresponding state while only having a StructEntry
  public void showStateWithStructEntry(StructEntry entry)
  {
    int stateNrToShow = 0;
    int transNrToShow = 0;

    // we can have states, triggers, transitions and actions
    if (entry instanceof State) {
      stateNrToShow = ((State) entry).getNumber();
      transNrToShow = ((State) entry).getFirstTrans();
    }
    else if (entry instanceof Transition) {
      int transnr = ((Transition) entry).getNumber();
      stateNrToShow = findStateForTrans(transnr);
      transNrToShow = transnr;
    }
    else if (entry instanceof StateTrigger) {
      int triggerOffset = ((StateTrigger) entry).getOffset();
      int nr = 0;
      for (StateTrigger trig : staTriList) {
        if (trig.getOffset() == triggerOffset) {
          break;
        }
        nr++;
      }

      for (State state : stateList) {
        if (state.getTriggerIndex() == nr) {
          stateNrToShow = state.getNumber();
          transNrToShow = state.getFirstTrans();
          break;
        }
      }
    }
    else if (entry instanceof ResponseTrigger) {
      int triggerOffset = ((ResponseTrigger) entry).getOffset();
      int nr = 0;
      for (ResponseTrigger trig : transTriList) {
        if (trig.getOffset() == triggerOffset) {
          break;
        }
        nr++;
      }

      for (Transition trans : transList) {
        if (trans.getTriggerIndex() == nr) {
          transNrToShow = trans.getNumber();
          stateNrToShow = findStateForTrans(transNrToShow);
        }
      }
    }
    else if (entry instanceof Action) {
      int actionOffset = ((Action) entry).getOffset();
      int nr = 0;
      for (Action action : actionList) {
        if (action.getOffset() == actionOffset) {
          break;
        }
        nr++;
      }

      for (Transition trans : transList) {
        if (trans.getActionIndex() == nr) {
          transNrToShow = trans.getNumber();
          stateNrToShow = findStateForTrans(transNrToShow);
        }
      }
    }
    else if (entry instanceof StringRef) {
      // this can happen with the dlg search
      // check all states and transitions
      int strref = ((StringRef) entry).getValue();
      boolean found = false;
      for (State state : stateList) {
        if (state.getResponse().getValue() == strref) {
          stateNrToShow = state.getNumber();
          transNrToShow = state.getFirstTrans();
          found = true;
          break;
        }
      }
      if (!found) {
        for (Transition trans : transList) {
          if (trans.getAssociatedText().getValue() == strref) {
            transNrToShow = trans.getNumber();
            stateNrToShow = findStateForTrans(transNrToShow);
            break;
          }
          else if (trans.getJournalEntry().getValue() == strref) {
            transNrToShow = trans.getNumber();
            stateNrToShow = findStateForTrans(transNrToShow);
            break;
          }
        }
      }
    }

    showState(stateNrToShow);
    showTransition(transNrToShow);
  }

  private int findStateForTrans(int transnr)
  {
    for (State state : stateList) {
      if ((transnr >= state.getFirstTrans()) &&
          (transnr < (state.getFirstTrans() + state.getTransCount()))) {
        return state.getNumber();
      }
    }
    // default
    return 0;
  }

  private void showState(int nr)
  {
    if (currentstate != null) {
      currentstate.removeTableModelListener(this);
    }
    currentstate = stateList.get(nr);
    currentstate.addTableModelListener(this);
    bostate.setTitle("State " + nr + '/' + (stateList.size() - 1));
    stateTextPanel.display(currentstate, nr);
    tfState.setText(String.valueOf(nr));
    outerpanel.repaint();
    if (currentstate.getTriggerIndex() != 0xffffffff) {
      stateTriggerPanel.display(staTriList.get(currentstate.getTriggerIndex()),
                                currentstate.getTriggerIndex());
    } else {
      stateTriggerPanel.clearDisplay();
    }
    buttonPanel.getControlByType(CtrlPrevState).setEnabled(nr > 0);
    buttonPanel.getControlByType(CtrlNextState).setEnabled(nr + 1 < stateList.size());
  }

  private void showTransition(int nr)
  {
    if (currenttransition != null) {
      currenttransition.removeTableModelListener(this);
    }
    currenttransition = transList.get(nr);
    currenttransition.addTableModelListener(this);
    botrans.setTitle("Response " + (nr - currentstate.getFirstTrans()) +
                     '/' + (currentstate.getTransCount() - 1));
    tfResponse.setText(String.valueOf(nr - currentstate.getFirstTrans()));
    outerpanel.repaint();
    transTextPanel.display(currenttransition, nr);
    if (currenttransition.getFlag().isFlagSet(1)) {
      transTriggerPanel.display(transTriList.get(currenttransition.getTriggerIndex()),
                                currenttransition.getTriggerIndex());
    } else {
      transTriggerPanel.clearDisplay();
    }
    if (currenttransition.getFlag().isFlagSet(2)) {
      transActionPanel.display(actionList.get(currenttransition.getActionIndex()),
                               currenttransition.getActionIndex());
    } else {
      transActionPanel.clearDisplay();
    }
    buttonPanel.getControlByType(CtrlSelect).setEnabled(!currenttransition.getFlag().isFlagSet(3));
    buttonPanel.getControlByType(CtrlPrevTrans).setEnabled(nr > currentstate.getFirstTrans());
    buttonPanel.getControlByType(CtrlNextTrans)
      .setEnabled(nr - currentstate.getFirstTrans() + 1 < currentstate.getTransCount());
  }

  private void updateViewerLists()
  {
    stateList.clear();
    transList.clear();
    staTriList.clear();
    transTriList.clear();
    actionList.clear();
    for (int i = 0; i < dlg.getFieldCount(); i++) {
      StructEntry entry = dlg.getField(i);
      if (entry instanceof State) {
        stateList.add((State)entry);
      } else if (entry instanceof Transition) {
        transList.add((Transition)entry);
      } else if (entry instanceof StateTrigger) {
        staTriList.add((StateTrigger)entry);
      } else if (entry instanceof ResponseTrigger) {
        transTriList.add((ResponseTrigger)entry);
      } else if (entry instanceof Action) {
        actionList.add((Action)entry);
      }
    }
  }

  private void showExternState(DlgResource newdlg, int state, boolean isUndo) {

    alive = false;
    Container window = getTopLevelAncestor();
    if (window instanceof ViewFrame && window.isVisible()) {
      ((ViewFrame) window).setViewable(newdlg);
    } else {
      NearInfinity.getInstance().setViewable(newdlg);
    }

    Viewer newdlg_viewer = (Viewer)newdlg.getViewerTab(0);
    if (isUndo) {
      newdlg_viewer.alive = true;
      newdlg_viewer.repaint(); // only necessary when dlg is in extra window
    } else {
      newdlg_viewer.setUndoDlg(this.dlg);
      newdlg_viewer.showState(state);
      newdlg_viewer.showTransition(newdlg_viewer.currentstate.getFirstTrans());
    }

    // make sure the viewer tab is selected
    JTabbedPane parent = (JTabbedPane) newdlg_viewer.getParent();
    parent.getModel().setSelectedIndex(parent.indexOfComponent(newdlg_viewer));
  }

// -------------------------- INNER CLASSES --------------------------

  private final class DlgPanel extends JPanel implements ActionListener
  {
    private final JButton bView = new JButton(Icons.getIcon(Icons.ICON_ZOOM_16));
    private final JButton bGoto = new JButton(Icons.getIcon(Icons.ICON_ROW_INSERT_AFTER_16));
    private final JButton bPlay = new JButton(Icons.getIcon(Icons.ICON_VOLUME_16));
    private final ScriptTextArea textArea = new ScriptTextArea();
    private final JLabel label = new JLabel();
    private final String title;
    private AbstractStruct struct;
    private StructEntry structEntry;

    private DlgPanel(String title, boolean viewable)
    {
      this(title, viewable, false);
    }

    private DlgPanel(String title, boolean viewable, boolean useHighlighting)
    {
      this.title = title;
      bView.setMargin(new Insets(0, 0, 0, 0));
      bView.addActionListener(this);
      bGoto.setMargin(bView.getMargin());
      bGoto.addActionListener(this);
      bPlay.setMargin(bView.getMargin());
      bPlay.addActionListener(this);
      bView.setToolTipText("View/Edit");
      bGoto.setToolTipText("Select attribute");
      bPlay.setToolTipText("Open associated sound");
      if (!useHighlighting) {
        textArea.applyExtendedSettings(null, null);
      }
      textArea.setEditable(false);
      textArea.setHighlightCurrentLine(false);
      if (viewable) {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
      }
      textArea.setMargin(new Insets(3, 3, 3, 3));
      textArea.setFont(BrowserMenuBar.getInstance().getScriptFont());
      InfinityScrollPane scroll = new InfinityScrollPane(textArea, true);
      if (!useHighlighting) {
        scroll.setLineNumbersEnabled(false);
      }

      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      setLayout(gbl);

      gbc.insets = new Insets(0, 3, 0, 0);
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      gbc.weighty = 0.0;
      gbc.anchor = GridBagConstraints.WEST;
      gbl.setConstraints(bGoto, gbc);
      add(bGoto);
      if (viewable) {
        gbl.setConstraints(bView, gbc);
        add(bView);
        gbl.setConstraints(bPlay, gbc);
        add(bPlay);
      }

      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.insets.right = 3;
      gbl.setConstraints(label, gbc);
      add(label);

      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      gbl.setConstraints(scroll, gbc);
      add(scroll);
    }

    private void display(State state, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(true);
      bGoto.setEnabled(true);
      struct = state;
      structEntry = state;
      StringRef response = state.getResponse();
      textArea.setText(response.toString() + "\n(StrRef: " + response.getValue() + ')');
      bPlay.setEnabled(StringResource.getWavResource(response.getValue()) != null);
      textArea.setCaretPosition(0);
    }

    private void display(Transition trans, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(true);
      bGoto.setEnabled(true);
      struct = trans;
      structEntry = trans;
      StringRef assText = trans.getAssociatedText();
      StringRef jouText = trans.getJournalEntry();
      String text = "";
      if (trans.getFlag().isFlagSet(0))
        text = assText.toString() + "\n(StrRef: " + assText.getValue() + ")\n";
      if (trans.getFlag().isFlagSet(4))
        text += "\nJournal entry:\n" + jouText.toString() + "\n(StrRef: " + jouText.getValue() + ')';
      bPlay.setEnabled(StringResource.getWavResource(assText.getValue()) != null);
      textArea.setText(text);
      textArea.setCaretPosition(0);
    }

    private void display(AbstractCode trigger, int number)
    {
      label.setText(title + " (" + number + ')');
      bView.setEnabled(false);
      bPlay.setEnabled(false);
      bGoto.setEnabled(true);
      structEntry = trigger;
      Compiler compiler = new Compiler(trigger.toString(),
                                       (trigger instanceof Action) ? Compiler.ScriptType.ACTION :
                                                                     Compiler.ScriptType.TRIGGER);
      String code = compiler.getCode();
      try {
        if (compiler.getErrors().size() == 0) {
          Decompiler decompiler = new Decompiler(code, true);
          if (trigger instanceof Action) {
            decompiler.setScriptType(Decompiler.ScriptType.ACTION);
          } else {
            decompiler.setScriptType(Decompiler.ScriptType.TRIGGER);
          }
          textArea.setText(decompiler.getSource());
        } else {
          textArea.setText(trigger.toString());
        }
      } catch (Exception e) {
        textArea.setText(trigger.toString());
      }
      textArea.setCaretPosition(0);
    }

    private void clearDisplay()
    {
      label.setText(title + " (-)");
      textArea.setText("");
      bView.setEnabled(false);
      bGoto.setEnabled(false);
      struct = null;
      structEntry = null;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == bView) {
        new ViewFrame(getTopLevelAncestor(), struct);
      } else if (event.getSource() == bGoto) {
        dlg.getViewer().selectEntry(structEntry.getName());
      } else if (event.getSource() == bPlay) {
        StringRef text = null;
        if (struct instanceof State) {
          text = ((State)struct).getResponse();
        } else if (struct instanceof Transition) {
          text = ((Transition)struct).getAssociatedText();
        }
        if (text != null) {
          String resourceName = StringResource.getWavResource(text.getValue());
          if (resourceName != null) {
            ResourceEntry entry = ResourceFactory.getResourceEntry(resourceName + ".WAV");
            new ViewFrame(getTopLevelAncestor(), ResourceFactory.getResource(entry));
          }
        }
      }
    }
  }
}


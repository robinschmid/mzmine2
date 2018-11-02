package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.actions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductType;

public class CombineAdductsDialog extends JDialog implements MouseListener {

  public static void main(String[] args) {
    CombineAdductsDialog d = new CombineAdductsDialog(new ESIAdductType[0]);
    d.setVisible(true);
  }

  private JList<ESIAdductType> adducts, combine;

  // new types to be added
  private List<ESIAdductType> newTypes = new ArrayList<>();

  public CombineAdductsDialog(ESIAdductType[] add) {
    setModalityType(ModalityType.APPLICATION_MODAL);
    setSize(650, 500);

    JPanel main = new JPanel();
    main.setLayout(new MigLayout("", "[grow][150px,center][grow]", "[grow]"));
    this.getRootPane().setLayout(new BorderLayout());
    this.getRootPane().add(main, BorderLayout.CENTER);

    JScrollPane scrollPane = new JScrollPane();
    main.add(scrollPane, "cell 0 0,grow");

    adducts = new JList<>();
    scrollPane.setViewportView(adducts);

    JPanel panel_1 = new JPanel();
    main.add(panel_1, "cell 1 0,grow");
    panel_1.setLayout(new MigLayout("", "[]", "[][][][][][]"));

    JButton button = new JButton(">");
    panel_1.add(button, "cell 0 0,growx");
    button.addActionListener(e -> add(adducts.getSelectedValuesList()));

    JButton button_1 = new JButton("<");
    button_1.addActionListener(e -> remove(combine.getSelectedValuesList()));
    panel_1.add(button_1, "cell 0 1,growx");

    JButton button_2 = new JButton("<<");
    button_2.addActionListener(e -> ((DefaultListModel) combine.getModel()).removeAllElements());
    panel_1.add(button_2, "cell 0 2,growx");

    JButton btnAdd = new JButton("add");
    btnAdd.addActionListener(e -> createCombined());
    panel_1.add(btnAdd, "cell 0 3,growx");

    Component verticalStrut = Box.createVerticalStrut(20);
    panel_1.add(verticalStrut, "cell 0 4");

    JButton btnFinish = new JButton("finish");
    btnFinish.addActionListener(e -> finish());
    panel_1.add(btnFinish, "cell 0 5");

    JScrollPane scrollPane_1 = new JScrollPane();
    main.add(scrollPane_1, "cell 2 0,grow");

    combine = new JList<>(new DefaultListModel<ESIAdductType>());
    scrollPane_1.setViewportView(combine);
    combine.addMouseListener(this);

    // add all
    DefaultListModel<ESIAdductType> model = new DefaultListModel<>();
    Arrays.stream(add).forEach(a -> model.addElement(a));
    adducts.setModel(model);
    adducts.addMouseListener(this);
  }

  private void finish() {
    setVisible(false);
  }

  private void createCombined() {
    DefaultListModel<ESIAdductType> model = (DefaultListModel<ESIAdductType>) combine.getModel();
    ESIAdductType[] com = new ESIAdductType[model.size()];
    for (int i = 0; i < com.length; i++) {
      com[i] = model.get(i);
    }
    ESIAdductType nt = new ESIAdductType(com);
    newTypes.add(nt);
    // add to adducts
    DefaultListModel addModel = (DefaultListModel) adducts.getModel();
    addModel.addElement(nt);
  }

  public List<ESIAdductType> getNewTypes() {
    return newTypes;
  }

  private void add(List<ESIAdductType> list) {
    DefaultListModel model = (DefaultListModel) combine.getModel();
    list.stream().forEach(e -> model.addElement(e));
  }

  private void add(ESIAdductType e) {
    DefaultListModel model = (DefaultListModel) combine.getModel();
    model.addElement(e);
  }

  private void remove(List<ESIAdductType> list) {
    DefaultListModel model = (DefaultListModel) combine.getModel();
    list.stream().forEach(e -> model.removeElement(e));
  }

  // double click on element
  @Override
  public void mouseClicked(MouseEvent evt) {
    JList list = (JList) evt.getSource();
    if (evt.getClickCount() == 2) {
      // Double-click detected
      int index = list.locationToIndex(evt.getPoint());
      add((ESIAdductType) list.getModel().getElementAt(index));
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {}

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}
}

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableToGraph {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TableToGraph::new);
    }

    // ===== GUI =====
    private JFrame frame;
    private JTextArea xArea, yArea;
    private JLabel xHeader, yHeader;
    private JTextField titleField, xLabelField, yLabelField;
    private JRadioButton niceBtn, exactBtn;

    public TableToGraph() {
        frame = new JFrame("PlotPoint Table → Graph");
        frame.setIconImage(new ImageIcon(TableToGraph.class.getResource("/plotpoint.png")).getImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);

        frame.setJMenuBar(buildMenuBar());

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Left: data entry
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        titleField = new JTextField(10);
        JScrollPane titleScroll = new JScrollPane(titleField);
        labeled(left, "Plot title: ", titleScroll);

        JPanel labelsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        xLabelField = new JTextField("Time (s)");
        yLabelField = new JTextField("Velocity (m/s)");
        labelsRow.add(labeledPane("X axis label:", xLabelField));
        labelsRow.add(labeledPane("Y axis label:", yLabelField));
        left.add(labelsRow);
        left.add(Box.createVerticalStrut(10));

        // Live captions for data text areas
        xHeader = new JLabel("X values (" + xLabelField.getText() + ")", SwingConstants.LEFT);
        yHeader = new JLabel("Y values (" + yLabelField.getText() + ")", SwingConstants.LEFT);

        xArea = new JTextArea(0, 0);
        yArea = new JTextArea(0, 0);

        JPanel areasRow = new JPanel(new GridLayout(1, 2, 12, 0));
        areasRow.add(labeledPane(xHeader, new JScrollPane(xArea)));
        areasRow.add(labeledPane(yHeader, new JScrollPane(yArea)));
        left.add(areasRow);
        left.add(Box.createVerticalStrut(12));

        // Tick mode + buttons
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        niceBtn = new JRadioButton("Nice ticks (rounded)", true);
        exactBtn = new JRadioButton("Exact ticks (use your values)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(niceBtn);
        bg.add(exactBtn);
        bottomRow.add(niceBtn);
        bottomRow.add(exactBtn);

        JButton plotBtn = new JButton("Plot");
        plotBtn.addActionListener(e -> doPlot(false));
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            xArea.setText("");
            yArea.setText("");
        });
        JButton sampleBtn = new JButton("Load Sample");
        sampleBtn.addActionListener(e -> loadSample());

        bottomRow.add(Box.createHorizontalStrut(12));
        bottomRow.add(plotBtn);
        bottomRow.add(clearBtn);
        bottomRow.add(sampleBtn);

        left.add(bottomRow);

        // Right: live preview panel placeholder (empty) – we only pop plots in new frames.
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("Use the controls at left then click Plot.", SwingConstants.CENTER),
                  BorderLayout.CENTER);

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST); // nothing heavy here; keep main frame snappy
        frame.setContentPane(root);

        // Live update the headers when labels change
        DocumentListener live = new DocumentListener() {
            private void go() {
                xHeader.setText("X values (" + xLabelField.getText().trim() + ")");
                yHeader.setText("Y values (" + yLabelField.getText().trim() + ")");
            }
            public void insertUpdate(DocumentEvent e) { go(); }
            public void removeUpdate(DocumentEvent e) { go(); }
            public void changedUpdate(DocumentEvent e) { go(); }
        };
        xLabelField.getDocument().addDocumentListener(live);
        yLabelField.getDocument().addDocumentListener(live);

        loadSample(); // start with data in the boxes
        frame.setVisible(true);
    }

    // Build menu bar with Options→Colors… (line/points/grid/background/axis/ticks)
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu options = new JMenu("Options");

        JMenu colors = new JMenu("Colors…");

        JMenuItem lineColor = new JMenuItem("Line color");
        JMenuItem pointColor = new JMenuItem("Point color");
        JMenuItem gridColor = new JMenuItem("Grid color");
        JMenuItem bgColor = new JMenuItem("Background color");
        JMenuItem axisColor = new JMenuItem("Axis color");
        JMenuItem tickColor = new JMenuItem("Tick label color");

        lineColor.addActionListener(e -> PlotPanel.USER_COLORS.line = choose(PlotPanel.USER_COLORS.line));
        pointColor.addActionListener(e -> PlotPanel.USER_COLORS.point = choose(PlotPanel.USER_COLORS.point));
        gridColor.addActionListener(e -> PlotPanel.USER_COLORS.grid = choose(PlotPanel.USER_COLORS.grid));
        bgColor.addActionListener(e -> PlotPanel.USER_COLORS.background = choose(PlotPanel.USER_COLORS.background));
        axisColor.addActionListener(e -> PlotPanel.USER_COLORS.axis = choose(PlotPanel.USER_COLORS.axis));
        tickColor.addActionListener(e -> PlotPanel.USER_COLORS.ticks = choose(PlotPanel.USER_COLORS.ticks));

        colors.add(lineColor);
        colors.add(pointColor);
        colors.add(gridColor);
        colors.add(bgColor);
        colors.add(axisColor);
        colors.add(tickColor);

        options.add(colors);
        mb.add(options);
        return mb;
    }

    private static Color choose(Color current) {
        Color c = JColorChooser.showDialog(null, "Choose color", current);
        return (c == null) ? current : c;
    }

    private void loadSample() {
        // Example from your “not nice” sample
        double[] sx = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] sy = {0.851, 0.851, 0.851, 0.851, 0.851, 0.851, 0.851, 0.851};
        xArea.setText(join(sx));
        yArea.setText(join(sy));
        titleField.setText("Velocity (m/s) vs Time (s)");
        xLabelField.setText("Time (s)");
        yLabelField.setText("Velocity (m/s)");
    }

    private static String join(double[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private void doPlot(boolean reuseWindow) {
        double[] x = parseDoubles(xArea.getText());
        double[] y = parseDoubles(yArea.getText());
        if (x == null || y == null || x.length < 2 || y.length < 2 || x.length != y.length) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter comma-separated numbers for X and Y with the same length (≥ 2).",
                    "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String xLabel = textOrDefault(xLabelField.getText(), "X");
        String yLabel = textOrDefault(yLabelField.getText(), "Y");
        String title  = textOrDefault(titleField.getText(), yLabel + " vs " + xLabel);

        boolean niceTicks = niceBtn.isSelected();

        PlotPanel panel = new PlotPanel(x, y, xLabel, yLabel, niceTicks);
        JFrame f = new JFrame(title);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.setSize(980, 640);
        f.setLocationByPlatform(true);
        f.setContentPane(panel);
        f.setVisible(true);
    }

    private static String textOrDefault(String s, String d) {
        String t = (s == null) ? "" : s.trim();
        return t.isEmpty() ? d : t;
    }

    private static double[] parseDoubles(String csv) {
        if (csv == null) return null;
        List<Double> out = new ArrayList<>();
        for (String part : csv.split("[,\\s]+")) {
            if (part.isEmpty()) continue;
            try {
                out.add(Double.parseDouble(part));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static JPanel labeledPane(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static JPanel labeledPane(JLabel label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.add(label, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static void labeled(JPanel parent, String label, JComponent c) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(c, BorderLayout.CENTER);
        parent.add(row);
        parent.add(Box.createVerticalStrut(8));
    }

    // ===== Plot Panel =====
    static class PlotPanel extends JPanel {

        // public color palette modified via Options menu
        static class Palette {
            Color line = new Color(220, 0, 0);
            Color point = new Color(40, 150, 255);
            Color grid = new Color(190, 190, 190);
            Color background = Color.WHITE;
            Color axis = Color.BLACK;
            Color ticks = new Color(40, 40, 40);
        }
        static final Palette USER_COLORS = new Palette();

        // padding & metrics
        private static final int TOP_PAD = 50;
        private static final int RIGHT_PAD = 30;
        private static final int BOTTOM_PAD = 55;
        private static final int LEFT_PAD = 80;            // more room for y-tick labels
        private static final int TICK_LEN = 6;
        private static final int Y_TICK_LABEL_GAP = 10;    // gap between y-axis and label text
        private static final int POINT_DIAM = 8;

        // tick label formatting
        private static final DecimalFormat TICK_FMT = new DecimalFormat("0.###");

        // data
        private final double[] x;
        private final double[] y;
        private final String xLabel, yLabel;
        private final boolean niceTicks;

        public PlotPanel(double[] x, double[] y, String xLabel, String yLabel, boolean niceTicks) {
            if (x == null || y == null || x.length != y.length || x.length < 2) {
                throw new IllegalArgumentException("x & y must be same length (>= 2)");
            }
            this.x = x.clone();
            this.y = y.clone();
            this.xLabel = xLabel;
            this.yLabel = yLabel;
            this.niceTicks = niceTicks;
            setBackground(USER_COLORS.background);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(USER_COLORS.background);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int left = LEFT_PAD;
            int right = w - RIGHT_PAD;
            int top = TOP_PAD;
            int bottom = h - BOTTOM_PAD;

            // data ranges
            double xMin = Arrays.stream(x).min().orElse(0);
            double xMax = Arrays.stream(x).max().orElse(1);
            if (xMin == xMax) { // similar protection for x just in case
                double pad = Math.max(1e-3, Math.abs(xMin) * 0.10);
                xMin -= pad; xMax += pad;
            }

            double yMin = Arrays.stream(y).min().orElse(0);
            double yMax = Arrays.stream(y).max().orElse(1);
            // ---- FIX 2: expand a flat range so horizontal lines are visible
            double[] yr = expandRange(yMin, yMax);
            yMin = yr[0]; yMax = yr[1];

            // ticks
            double[] xTicks, yTicks;
            if (niceTicks) {
                xTicks = niceTicks(xMin, xMax, 8);
                yTicks = niceTicks(yMin, yMax, 8);
            } else {
                xTicks = exactTicksFromData(x);
                yTicks = exactTicksFromData(y);
            }

            // axes
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(USER_COLORS.axis);
            g2.drawLine(left, bottom, right, bottom);
            g2.drawLine(left, bottom, left, top);

            // grid
            g2.setColor(USER_COLORS.grid);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2f, 6f}, 0f));
            for (double xv : xTicks) {
                int tx = map(xv, xMin, xMax, left, right);
                g2.drawLine(tx, top, tx, bottom);
            }
            for (double yv : yTicks) {
                int ty = map(yv, yMin, yMax, bottom, top);
                g2.drawLine(left, ty, right, ty);
            }

            // ticks & tick labels
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(USER_COLORS.axis);
            for (double xv : xTicks) {
                int tx = map(xv, xMin, xMax, left, right);
                g2.drawLine(tx, bottom, tx, bottom + TICK_LEN);
            }
            for (double yv : yTicks) {
                int ty = map(yv, yMin, yMax, bottom, top);
                g2.drawLine(left - TICK_LEN, ty, left, ty);
            }

            g2.setColor(USER_COLORS.ticks);
            FontMetrics fm = g2.getFontMetrics();
            // ---- FIX 1: format with DecimalFormat + put a gap from the axis
            for (double xv : xTicks) {
                int tx = map(xv, xMin, xMax, left, right);
                String s = fmt(xv);
                int sw = fm.stringWidth(s);
                g2.drawString(s, tx - sw / 2, bottom + TICK_LEN + fm.getAscent() + 2);
            }
            for (double yv : yTicks) {
                int ty = map(yv, yMin, yMax, bottom, top);
                String s = fmt(yv);
                int sw = fm.stringWidth(s);
                int lx = left - TICK_LEN - Y_TICK_LABEL_GAP - sw;
                g2.drawString(s, lx, ty + fm.getAscent() / 2 - 2);
            }

            // axis labels
            g2.setColor(Color.DARK_GRAY);
            // x label
            String xCap = xLabel;
            int xCapW = fm.stringWidth(xCap);
            g2.drawString(xCap, (left + right - xCapW) / 2, h - 8);

            // y label (rotated), add extra spacing automatically
            String yCap = yLabel;
            g2.translate(14, (top + bottom + fm.stringWidth(yCap)) / 2);
            g2.rotate(-Math.PI / 2);
            g2.drawString(yCap, 0, 0);
            g2.rotate(Math.PI / 2);
            g2.translate(-14, -(top + bottom + fm.stringWidth(yCap)) / 2);

            // data polyline
            g2.setClip(new Rectangle(left, top, right - left, bottom - top));
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(USER_COLORS.line);
            int px = map(x[0], xMin, xMax, left, right);
            int py = map(y[0], yMin, yMax, bottom, top);
            for (int i = 1; i < x.length; i++) {
                int cx = map(x[i], xMin, xMax, left, right);
                int cy = map(y[i], yMin, yMax, bottom, top);
                g2.drawLine(px, py, cx, cy);
                px = cx; py = cy;
            }

            // points
            g2.setColor(USER_COLORS.point);
            for (int i = 0; i < x.length; i++) {
                int cx = map(x[i], xMin, xMax, left, right);
                int cy = map(y[i], yMin, yMax, bottom, top);
                Shape dot = new Ellipse2D.Double(cx - POINT_DIAM / 2.0, cy - POINT_DIAM / 2.0, POINT_DIAM, POINT_DIAM);
                g2.fill(dot);
            }

            g2.dispose();
        }

        // map data value to pixel
        private static int map(double v, double vMin, double vMax, int pMin, int pMax) {
            if (vMax == vMin) return (pMin + pMax) / 2;
            double t = (v - vMin) / (vMax - vMin);
            return (int) Math.round(pMin + t * (pMax - pMin));
        }

        private static String fmt(double v) {
            if (Math.abs(v) < 5e-4) v = 0.0; // snap tiny to zero, avoid "-0.0"
            return TICK_FMT.format(v);
        }

        // ---- FIX 2 helper: expand flat y range
        private static double[] expandRange(double min, double max) {
            if (min == max) {
                double base = Math.abs(min);
                double pad = Math.max(1e-3, (base > 1e-9 ? base * 0.10 : 1.0));
                return new double[]{min - pad, max + pad};
            }
            return new double[]{min, max};
        }

        // nice tick generation
        private static double[] niceTicks(double min, double max, int target) {
            if (min == max) { max = min + 1; }
            double range = niceNum(max - min, false);
            double step  = niceNum(range / (target <= 0 ? 8 : target), true);
            double niceMin = Math.floor(min / step) * step;
            double niceMax = Math.ceil (max / step) * step;
            List<Double> ticks = new ArrayList<>();
            for (double v = niceMin; v <= niceMax + 0.5 * step; v += step) ticks.add(v);
            return ticks.stream().mapToDouble(Double::doubleValue).toArray();
        }

        private static double niceNum(double range, boolean round) {
            double exp = Math.floor(Math.log10(range));
            double f = range / Math.pow(10, exp); // 1..10
            double nf;
            if (round) {
                if (f < 1.5) nf = 1;
                else if (f < 3) nf = 2;
                else if (f < 7) nf = 5;
                else nf = 10;
            } else {
                if (f <= 1) nf = 1;
                else if (f <= 2) nf = 2;
                else if (f <= 5) nf = 5;
                else nf = 10;
            }
            return nf * Math.pow(10, exp);
        }

        // exact ticks: unique sorted values from data
        private static double[] exactTicksFromData(double[] data) {
            return Arrays.stream(data).distinct().sorted().toArray();
        }
    }
}

package sky.netatmo.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import sky.netatmo.Device;
import sky.netatmo.Measure;
import sky.netatmo.MeasurementScale;
import sky.netatmo.MeasurementType;
import sky.netatmo.Measurer;
import sky.netatmo.Module;
import sky.netatmo.NetatmoException;
import sky.netatmo.Token;

public class SkyNetatmo extends JFrame
{
    private Token token;
    private final ChartPanel chartPanel;
    private final JComboBox deviceComboBox;
    private final JComboBox measureComboBox;
    private final JCheckBox realTimeCheckBox;
    private Date beginDate;
    private Date endDate;
    private Date lastAcquiredMeasure;
    private final Object lockObject;
    private final MeasureDatabase measureDatabase;
    private Measure[] tempMeasures;

    public SkyNetatmo()
    {
        super("SkyNetatmo by PJ Skyman");
        System.out.println("Preparing the interface...");
        ToolTipManager.sharedInstance().setDismissDelay(60000);
        lockObject=new Object();
        lastAcquiredMeasure=null;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setBounds(100,100,1000,600);
        JPanel mainPanel=new JPanel(new BorderLayout());
        setContentPane(mainPanel);
        endDate=new Date();
        beginDate=new Date(endDate.getTime()-60000L*60L*2L);
        JComboBox tempDeviceComboBox=null;
        JComboBox tempMeasureComboBox=null;
        MeasureDatabase tempMeasureDatabase=null;
        try
        {
            System.out.println("Retrieving the token...");
            token=Token.getToken("hhhhhhh","gggggg","a@a.com","ffffffffff");
            System.out.println("Ensuring that the token is valid...");
            ensureTokenIsValid();
            System.out.println("Getting the devices...");
            Device[] devices=token.getUser().getDevices();
            tempDeviceComboBox=new JComboBox(devices);
            tempDeviceComboBox.setRenderer(new DeviceRenderer());
            tempDeviceComboBox.setSelectedIndex(0);
            tempDeviceComboBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    if(e.getStateChange()==ItemEvent.DESELECTED)
                        return;
                    try
                    {
                        ensureTokenIsValid();
                        ItemListener[] itemListeners=measureComboBox.getItemListeners();
                        for(ItemListener itemListener:itemListeners)
                            measureComboBox.removeItemListener(itemListener);
                        measureComboBox.removeAllItems();
                        MeasureItem[] measureItems=createMeasureItems((Device)deviceComboBox.getSelectedItem());
                        for(MeasureItem measureItem:measureItems)
                            measureComboBox.addItem(measureItem);
                        measureComboBox.setSelectedIndex(1);
                        for(ItemListener itemListener:itemListeners)
                            measureComboBox.addItemListener(itemListener);
                        measureComboBox.setSelectedIndex(0);
                    }
                    catch(NetatmoException ex)
                    {
                        ex.printStackTrace();
                        System.exit(0);
                    }
                }
            });
            tempMeasureComboBox=new JComboBox(createMeasureItems(devices[0]));
            tempMeasureComboBox.setSelectedIndex(0);
            tempMeasureComboBox.setMaximumRowCount(20);
            tempMeasureComboBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    if(e.getStateChange()==ItemEvent.DESELECTED)
                        return;
                    try
                    {
                        ensureTokenIsValid();
                        updateGraphics(true);
                    }
                    catch(NetatmoException ex)
                    {
                        ex.printStackTrace();
                        System.exit(0);
                    }
                }
            });
            ArrayList<Measurer> measurers=new ArrayList<Measurer>();
            for(Device device:devices)
            {
                measurers.add(device);
                for(Module module:device.getModules())
                    measurers.add(module);
            }
            tempMeasureDatabase=new MeasureDatabase(measurers.toArray(new Measurer[measurers.size()]));
        }
        catch(NetatmoException e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        deviceComboBox=tempDeviceComboBox;
        measureComboBox=tempMeasureComboBox;
        measureDatabase=tempMeasureDatabase;
        JPanel commandPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        commandPanel.add(deviceComboBox);
        commandPanel.add(measureComboBox);
        realTimeCheckBox=new JCheckBox("Actualisation en temps-réel",true);
        realTimeCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(!realTimeCheckBox.isSelected())
                    return;
                try
                {
                    synchronized(lockObject)
                    {
                        long offset=endDate.getTime()-beginDate.getTime();
                        endDate=new Date(System.currentTimeMillis());
                        beginDate=new Date(endDate.getTime()-offset);
                    }
                    ensureTokenIsValid();
                    updateGraphics(true);
                    if(lastAcquiredMeasure!=null)
                        System.out.println("Date de la dernière mesure : "+SimpleDateFormat.getDateTimeInstance().format(lastAcquiredMeasure));
                }
                catch(NetatmoException ex)
                {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        });
        commandPanel.add(realTimeCheckBox);
        JButton zoomOut=new JButton("Zoom -");
        zoomOut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                synchronized(lockObject)
                {
                    realTimeCheckBox.setSelected(false);
                    long midTime=(beginDate.getTime()+endDate.getTime())/2L;
                    beginDate=new Date(midTime-(midTime-beginDate.getTime())*5L/3L);
                    endDate=new Date(midTime+(endDate.getTime()-midTime)*5L/3L);
                }
                try
                {
                    ensureTokenIsValid();
                    updateGraphics(true);
                }
                catch(NetatmoException ex)
                {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        });
        commandPanel.add(zoomOut);
        JButton zoomIn=new JButton("Zoom +");
        zoomIn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                synchronized(lockObject)
                {
                    realTimeCheckBox.setSelected(false);
                    long midTime=(beginDate.getTime()+endDate.getTime())/2L;
                    beginDate=new Date(midTime-(midTime-beginDate.getTime())*3L/5L);
                    endDate=new Date(midTime+(endDate.getTime()-midTime)*3L/5L);
                }
                try
                {
                    ensureTokenIsValid();
                    updateGraphics(true);
                }
                catch(NetatmoException ex)
                {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        });
        commandPanel.add(zoomIn);
        JButton goBefore=new JButton("<=");
        goBefore.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                synchronized(lockObject)
                {
                    realTimeCheckBox.setSelected(false);
                    long offset=endDate.getTime()-beginDate.getTime();
                    endDate=new Date(endDate.getTime()-offset/6L);
                    beginDate=new Date(beginDate.getTime()-offset/6L);
                }
                try
                {
                    ensureTokenIsValid();
                    updateGraphics(true);
                }
                catch(NetatmoException ex)
                {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        });
        commandPanel.add(goBefore);
        JButton goAfter=new JButton("=>");
        goAfter.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                synchronized(lockObject)
                {
                    realTimeCheckBox.setSelected(false);
                    long offset=endDate.getTime()-beginDate.getTime();
                    endDate=new Date(endDate.getTime()+offset/6L);
                    beginDate=new Date(beginDate.getTime()+offset/6L);
                }
                try
                {
                    ensureTokenIsValid();
                    updateGraphics(true);
                }
                catch(NetatmoException ex)
                {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        });
        commandPanel.add(goAfter);
        JButton closeButton=new JButton("Fermer");
        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        });
        commandPanel.add(closeButton);
        mainPanel.add(commandPanel,BorderLayout.NORTH);
        System.out.println("Constructing the graphics...");
        DefaultXYDataset tempDataset=new DefaultXYDataset();
        tempDataset.addSeries("",new double[][]{{0,1},{2,3}});
        chartPanel=new ChartPanel(new JFreeChart(new XYPlot(tempDataset,new DateAxis(),new NumberAxis(),new XYShapeRenderer())))
        {
            @Override
            public void paintComponent(Graphics g)
            {
                Graphics2D g2d=(Graphics2D)g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);//meilleur rapport qualité/performances
                g2d.setRenderingHint(RenderingHints.KEY_DITHERING,RenderingHints.VALUE_DITHER_ENABLE);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g);
            }
        };
        mainPanel.add(chartPanel,BorderLayout.CENTER);
        System.out.println("Launching the update thread...");
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    sleep(1000L);
                    while(true)
                    {
                        try
                        {
                            if(realTimeCheckBox.isSelected())
                                synchronized(lockObject)
                                {
                                    long offset=endDate.getTime()-beginDate.getTime();
                                    endDate=new Date();
                                    beginDate=new Date(endDate.getTime()-offset);
                                }
                            ensureTokenIsValid();
                            updateGraphics(true);
                            if(realTimeCheckBox.isSelected()&&lastAcquiredMeasure!=null)
                                System.out.println("Date de la dernière mesure : "+SimpleDateFormat.getDateTimeInstance().format(lastAcquiredMeasure));
                        }
                        catch(NetatmoException e)
                        {
                            e.printStackTrace();
                            System.exit(0);
                        }
                        sleep(200000L);
                    }
                }
                catch(InterruptedException e)
                {
                }
            }
        }.start();
        System.out.println("Launching the repaint thread...");
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    sleep(2500L);
                    while(true)
                    {
                        try
                        {
                            if(realTimeCheckBox.isSelected())
                                synchronized(lockObject)
                                {
                                    long offset=endDate.getTime()-beginDate.getTime();
                                    endDate=new Date();
                                    beginDate=new Date(endDate.getTime()-offset);
                                }
                            updateGraphics(false);
                        }
                        catch(NetatmoException e)
                        {
                            e.printStackTrace();
                            System.exit(0);
                        }
                        sleep(728L);
                    }
                }
                catch(InterruptedException e)
                {
                }
            }
        }.start();
        System.out.println("Interface is ok !");
//        try
//        {
//            Device device=token.getUser().getDevices()[0];
//            Measure[] array=device.getMeasures(MeasurementScale.MAX,new GregorianCalendar(2013,6,23,15,58,21).getTime(),new GregorianCalendar(2013,6,23,15,58,23).getTime(),MeasurementType.TEMPERATURE);
//            System.out.println("58 "+array.length);
//        }
//        catch(Exception e)
//        {
//            e.printStackTrace();
//        }
        setVisible(true);
    }

    private void updateGraphics(boolean updateMeasures) throws NetatmoException
    {
        MeasureItem selectedMeasureItem=(MeasureItem)measureComboBox.getSelectedItem();
        Device selectedDevice=(Device)deviceComboBox.getSelectedItem();
        if(selectedDevice==null||selectedMeasureItem==null)
            tempMeasures=new Measure[0];
        else
            if(updateMeasures||tempMeasures==null)
                synchronized(lockObject)
                {
                    long startTime=System.currentTimeMillis();
                    tempMeasures=measureDatabase.getMeasures(selectedMeasureItem.measurer,MeasurementScale.MAX,new Date(beginDate.getTime()-60000L*7L),new Date(endDate.getTime()+60000L*7L),selectedMeasureItem.measurementType);
                    System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date())+" : "+tempMeasures.length+" mesures affichées en "+(System.currentTimeMillis()-startTime)+" ms");
                }
        XYSeries series;
        if(selectedDevice==null||selectedMeasureItem==null)
            series=new XYSeries("");
        else
            series=new XYSeries(selectedDevice.getStationName()+", "+selectedMeasureItem.toString());
        double min=1e9d;
        double max=-1e9d;
        for(Measure measure:tempMeasures)
        {
            double value=measure.getValue();
            series.add(measure.getDate().getTime(),value);
            if(value<min)
                min=value;
            if(value>max)
                max=value;
        }
        XYSeriesCollection dataset=new XYSeriesCollection(series);
        DateAxis dateAxis=new DateAxis();
        dateAxis.setRange(beginDate,endDate);
        NumberAxis axis;
        double rangeMin;
        double rangeMax;
        if(selectedMeasureItem==null||selectedMeasureItem.measurementType==MeasurementType.TEMPERATURE)
        {
            axis=new TemperatureAxis();
            rangeMin=-10d;
            if(min<rangeMin)
                rangeMin=min;
            rangeMax=40d;
            if(max>rangeMax)
                rangeMax=max;
        }
        else
            if(selectedMeasureItem.measurementType==MeasurementType.HUMIDITY)
            {
                axis=new HumidityAxis();
                rangeMin=0d;
                rangeMax=100d;
            }
            else
                if(selectedMeasureItem.measurementType==MeasurementType.PRESSURE)
                {
                    axis=new PressureAxis();
                    rangeMin=980d;
                    if(min<rangeMin)
                        rangeMin=min;
                    rangeMax=1040d;
                    if(max>rangeMax)
                        rangeMax=max;
                }
                else
                    if(selectedMeasureItem.measurementType==MeasurementType.CO2)
                    {
                        axis=new CO2Axis();
                        rangeMin=0d;
                        if(min<rangeMin)
                            rangeMin=min;
                        rangeMax=3000d;
                        if(max>rangeMax)
                            rangeMax=max;
                    }
                    else
                        if(selectedMeasureItem.measurementType==MeasurementType.NOISE)
                        {
                            axis=new NoiseAxis();
                            rangeMin=30d;
                            if(min<rangeMin)
                                rangeMin=min;
                            rangeMax=100d;
                            if(max>rangeMax)
                                rangeMax=max;
                        }
                        else//mesure de type inconnu
                        {
                            axis=new NumberAxis();
                            rangeMin=min;
                            rangeMax=max;
                        }
        axis.setRange(rangeMin,rangeMax);
        XYSplineRenderer renderer=new XYSplineRenderer(12);
        renderer.setSeriesPaint(0,new Color(48,142,255));
        renderer.setSeriesShape(0,new Rectangle2D.Double(-1.5d,-1.5d,3d,3d));
        renderer.setSeriesStroke(0,new BasicStroke(1.5f));
        renderer.setSeriesToolTipGenerator(0,new XYToolTipGenerator()
        {
            public String generateToolTip(XYDataset dataset,int series,int item)
            {
                long date=((Double)dataset.getX(series,item)).longValue();
                double value=(Double)dataset.getY(series,item);
                return SimpleDateFormat.getDateTimeInstance().format(new Date(date))+" : "+value;
            }
        });
        XYPlot plot;
        if(selectedMeasureItem==null||selectedMeasureItem.measurementType==MeasurementType.TEMPERATURE)
            plot=new TemperaturePlot(dataset,dateAxis,axis,renderer);
        else
            if(selectedMeasureItem.measurementType==MeasurementType.HUMIDITY)
                plot=new HumidityPlot(dataset,dateAxis,axis,renderer);
            else
                if(selectedMeasureItem.measurementType==MeasurementType.PRESSURE)
                    plot=new PressurePlot(dataset,dateAxis,axis,renderer);
                else
                    if(selectedMeasureItem.measurementType==MeasurementType.CO2)
                        plot=new CO2Plot(dataset,dateAxis,axis,renderer);
                    else
                        if(selectedMeasureItem.measurementType==MeasurementType.NOISE)
                            plot=new NoisePlot(dataset,dateAxis,axis,renderer);
                        else//mesure de type inconnu
                            plot=new XYPlot(dataset,dateAxis,axis,renderer);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
        chartPanel.setChart(new JFreeChart(plot));
        try
        {
            lastAcquiredMeasure=tempMeasures[tempMeasures.length-1].getDate();
        }
        catch(IndexOutOfBoundsException e)
        {
            lastAcquiredMeasure=null;
        }
    }

    private void updateGraphicsDirectFromServer() throws NetatmoException
    {
        ArrayList<Measure> measures=new ArrayList<Measure>();
        MeasureItem selectedMeasureItem=(MeasureItem)measureComboBox.getSelectedItem();
        long startTime=System.currentTimeMillis();
        synchronized(lockObject)
        {
            Measure[] array=selectedMeasureItem.measurer.getMeasures(MeasurementScale.MAX,beginDate,endDate,selectedMeasureItem.measurementType);
            for(Measure measure:array)
                measures.add(measure);
            while(array.length==1024)
            {
                array=selectedMeasureItem.measurer.getMeasures(MeasurementScale.MAX,new Date(measures.get(measures.size()-1).getDate().getTime()+300000L),endDate,selectedMeasureItem.measurementType);
                for(Measure measure:array)
                    measures.add(measure);
            }
            if(!measures.isEmpty())
                System.out.println("        "+measures.size()+" mesures lues en tout sur le serveur");
        }
        System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date())+" : "+measures.size()+" mesures affichées en "+(System.currentTimeMillis()-startTime)+" ms");
        XYSeries series=new XYSeries(selectedMeasureItem.toString());
        double min=1e9d;
        double max=-1e9d;
        for(Measure measure:measures)
        {
            double value=measure.getValue();
            series.add(measure.getDate().getTime(),value);
            if(value<min)
                min=value;
            if(value>max)
                max=value;
        }
        XYSeriesCollection dataset=new XYSeriesCollection(series);
        DateAxis dateAxis=new DateAxis();
        dateAxis.setRange(beginDate,endDate);
        NumberAxis axis;
        double rangeMin;
        double rangeMax;
        if(selectedMeasureItem.measurementType==MeasurementType.TEMPERATURE)
        {
            axis=new TemperatureAxis();
            rangeMin=-10d;
            if(min<rangeMin)
                rangeMin=min;
            rangeMax=40d;
            if(max>rangeMax)
                rangeMax=max;
        }
        else
            if(selectedMeasureItem.measurementType==MeasurementType.HUMIDITY)
            {
                axis=new HumidityAxis();
                rangeMin=0d;
                rangeMax=100d;
            }
            else
                if(selectedMeasureItem.measurementType==MeasurementType.PRESSURE)
                {
                    axis=new PressureAxis();
                    rangeMin=980d;
                    if(min<rangeMin)
                        rangeMin=min;
                    rangeMax=1040d;
                    if(max>rangeMax)
                        rangeMax=max;
                }
                else
                    if(selectedMeasureItem.measurementType==MeasurementType.CO2)
                    {
                        axis=new CO2Axis();
                        rangeMin=0d;
                        if(min<rangeMin)
                            rangeMin=min;
                        rangeMax=3000d;
                        if(max>rangeMax)
                            rangeMax=max;
                    }
                    else
                        if(selectedMeasureItem.measurementType==MeasurementType.NOISE)
                        {
                            axis=new NoiseAxis();
                            rangeMin=30d;
                            if(min<rangeMin)
                                rangeMin=min;
                            rangeMax=100d;
                            if(max>rangeMax)
                                rangeMax=max;
                        }
                        else//mesure de type inconnu
                        {
                            axis=new NumberAxis();
                            rangeMin=min;
                            rangeMax=max;
                        }
        axis.setRange(rangeMin,rangeMax);
        XYLineAndShapeRenderer renderer=new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0,new Color(48,142,255));
        renderer.setSeriesShape(0,new Rectangle2D.Double(-1d,-1d,2d,2d));
        renderer.setSeriesToolTipGenerator(0,new XYToolTipGenerator()
        {
            public String generateToolTip(XYDataset dataset,int series,int item)
            {
                long date=((Double)dataset.getX(series,item)).longValue();
                double value=(Double)dataset.getY(series,item);
                return SimpleDateFormat.getDateTimeInstance().format(new Date(date))+" : "+value;
            }
        });
        XYPlot plot;
        if(selectedMeasureItem.measurementType==MeasurementType.TEMPERATURE)
            plot=new TemperaturePlot(dataset,dateAxis,axis,renderer);
        else
            if(selectedMeasureItem.measurementType==MeasurementType.HUMIDITY)
                plot=new HumidityPlot(dataset,dateAxis,axis,renderer);
            else
                if(selectedMeasureItem.measurementType==MeasurementType.PRESSURE)
                    plot=new PressurePlot(dataset,dateAxis,axis,renderer);
                else
                    if(selectedMeasureItem.measurementType==MeasurementType.CO2)
                        plot=new CO2Plot(dataset,dateAxis,axis,renderer);
                    else
                        if(selectedMeasureItem.measurementType==MeasurementType.NOISE)
                            plot=new NoisePlot(dataset,dateAxis,axis,renderer);
                        else//mesure de type inconnu
                            plot=new XYPlot(dataset,dateAxis,axis,renderer);
        chartPanel.setChart(new JFreeChart(plot));
        lastAcquiredMeasure=measures.get(measures.size()-1).getDate();
    }

    private MeasureItem[] createMeasureItems(Device device) throws NetatmoException
    {
        ArrayList<MeasureItem> measureItems=new ArrayList<MeasureItem>();
        for(MeasurementType measurementType:device.getCompatibleMeasurementTypes())
            measureItems.add(new MeasureItem(device,measurementType));
        for(Module module:device.getModules())
            for(MeasurementType measurementType:module.getCompatibleMeasurementTypes())
                measureItems.add(new MeasureItem(module,measurementType));
        return measureItems.toArray(new MeasureItem[measureItems.size()]);
    }

    private void ensureTokenIsValid() throws NetatmoException
    {
        synchronized(lockObject)
        {
            if(token.isExpired())
            {
                token=token.renewExpiredToken();
                System.out.println(SimpleDateFormat.getDateTimeInstance().format(new Date())+" : Le jeton a été renouvelé");
            }
        }
    }

    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        new SkyNetatmo();
    }

    private static class MeasureItem
    {
        private final Measurer measurer;
        private final MeasurementType measurementType;

        private MeasureItem(Measurer measurer,MeasurementType measurementType)
        {
            this.measurer=measurer;
            this.measurementType=measurementType;
        }

        @Override
        public String toString()
        {
            String s=measurer.getName()+" : ";
            if(measurementType==MeasurementType.TEMPERATURE)
                s+="Température";
            else
                if(measurementType==MeasurementType.HUMIDITY)
                    s+="Humidité";
                else
                    if(measurementType==MeasurementType.PRESSURE)
                        s+="Pression";
                    else
                        if(measurementType==MeasurementType.CO2)
                            s+="CO2";
                        else
                            if(measurementType==MeasurementType.NOISE)
                                s+="Bruit";
                            else
                                s+="?";
            return s;
        }
    }

    private static class DeviceRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list,Object value,int index,boolean isSelected,boolean cellHasFocus)
        {
            JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            label.setText(((Device)value).getStationName());
            return label;
        }
    }

    /*
     *
     * Température.
     *
     */

    private static class TemperaturePlot extends XYPlot
    {
        private static final Color MINUS_TEN_DEGREES_COLOR=new Color(192,192,255);
        private static final Color FOURTY_DEGREES_COLOR=new Color(255,192,192);

        private TemperaturePlot(XYDataset dataset,ValueAxis domainAxis,ValueAxis rangeAxis,XYItemRenderer renderer)
        {
            super(dataset,domainAxis,rangeAxis,renderer);
            setDomainGridlinePaint(Color.BLACK);
            setRangeGridlinePaint(Color.BLACK);
            setRangeMinorGridlinesVisible(true);
            setRangeMinorGridlinePaint(Color.GRAY);
        }

        @Override
        protected void fillBackground(Graphics2D g2,Rectangle2D area,PlotOrientation orientation)
        {
            ValueAxis rangeAxis=getRangeAxis();
            double minusTenDegreesOrdinate=rangeAxis.valueToJava2D(-10d,area,RectangleEdge.LEFT);
            double fourtyDegreesOrdinate=rangeAxis.valueToJava2D(40d,area,RectangleEdge.LEFT);
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),minusTenDegreesOrdinate),MINUS_TEN_DEGREES_COLOR,new Point2D.Double(area.getX(),fourtyDegreesOrdinate),FOURTY_DEGREES_COLOR));
            g2.fill(area);
        }
    }

    private static class TemperatureAxis extends NumberAxis
    {
        private TemperatureAxis()
        {
            setAutoRange(false);
        }

        @Override
        protected List refreshTicksVertical(Graphics2D g2,Rectangle2D dataArea,RectangleEdge edge)
        {
            ArrayList<NumberTick> ticks=new ArrayList<NumberTick>();
            for(double temperature=-50d;temperature<=100d;temperature+=1d)
                if(temperature%10d==0d)
                    ticks.add(new NumberTick(TickType.MAJOR,temperature,(int)temperature+" °C",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
                else
                    ticks.add(new NumberTick(TickType.MINOR,temperature,"",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
            return ticks;
        }
    }

    /*
     *
     * Humidité.
     *
     */

    private static class HumidityPlot extends XYPlot
    {
        private static final Color PERCENT_0_COLOR=new Color(255,192,192);
        private static final Color PERCENT_50_COLOR=new Color(192,255,192);
        private static final Color PERCENT_100_COLOR=new Color(255,192,192);

        private HumidityPlot(XYDataset dataset,ValueAxis domainAxis,ValueAxis rangeAxis,XYItemRenderer renderer)
        {
            super(dataset,domainAxis,rangeAxis,renderer);
            setDomainGridlinePaint(Color.BLACK);
            setRangeGridlinePaint(Color.BLACK);
            setRangeMinorGridlinesVisible(true);
            setRangeMinorGridlinePaint(Color.GRAY);
        }

        @Override
        protected void fillBackground(Graphics2D g2,Rectangle2D area,PlotOrientation orientation)
        {
            ValueAxis rangeAxis=getRangeAxis();
            double percent0Ordinate=rangeAxis.valueToJava2D(0d,area,RectangleEdge.LEFT);
            double percent50Ordinate=rangeAxis.valueToJava2D(50d,area,RectangleEdge.LEFT);
            double percent100Ordinate=rangeAxis.valueToJava2D(100d,area,RectangleEdge.LEFT);
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),percent0Ordinate),PERCENT_0_COLOR,new Point2D.Double(area.getX(),percent50Ordinate),PERCENT_50_COLOR));
            g2.fill(new Rectangle2D.Double(area.getX(),percent50Ordinate,area.getWidth(),percent0Ordinate-percent50Ordinate));
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),percent50Ordinate),PERCENT_50_COLOR,new Point2D.Double(area.getX(),percent100Ordinate),PERCENT_100_COLOR));
            g2.fill(new Rectangle2D.Double(area.getX(),percent100Ordinate,area.getWidth(),percent50Ordinate-percent100Ordinate));
        }
    }

    private static class HumidityAxis extends NumberAxis
    {
        private HumidityAxis()
        {
            setAutoRange(false);
        }

        @Override
        protected List refreshTicksVertical(Graphics2D g2,Rectangle2D dataArea,RectangleEdge edge)
        {
            ArrayList<NumberTick> ticks=new ArrayList<NumberTick>();
            for(double humidity=0d;humidity<=100d;humidity+=1d)
                if(humidity%10d==0d)
                    ticks.add(new NumberTick(TickType.MAJOR,humidity,(int)humidity+" %",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
                else
                    ticks.add(new NumberTick(TickType.MINOR,humidity,"",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
            return ticks;
        }
    }

    /*
     *
     * Pression.
     *
     */

    private static class PressurePlot extends XYPlot
    {
        private static final Color HPA_980_COLOR=new Color(255,192,192);
        private static final Color HPA_1013_3_COLOR=new Color(192,255,192);
        private static final Color HPA_1040_COLOR=new Color(255,192,192);

        private PressurePlot(XYDataset dataset,ValueAxis domainAxis,ValueAxis rangeAxis,XYItemRenderer renderer)
        {
            super(dataset,domainAxis,rangeAxis,renderer);
            setDomainGridlinePaint(Color.BLACK);
            setRangeGridlinePaint(Color.BLACK);
            setRangeMinorGridlinesVisible(true);
            setRangeMinorGridlinePaint(Color.GRAY);
        }

        @Override
        protected void fillBackground(Graphics2D g2,Rectangle2D area,PlotOrientation orientation)
        {
            ValueAxis rangeAxis=getRangeAxis();
            double hpa980Ordinate=rangeAxis.valueToJava2D(980d,area,RectangleEdge.LEFT);
            double hpa10133Ordinate=rangeAxis.valueToJava2D(1013.3d,area,RectangleEdge.LEFT);
            double hpa1040Ordinate=rangeAxis.valueToJava2D(1040d,area,RectangleEdge.LEFT);
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),hpa980Ordinate),HPA_980_COLOR,new Point2D.Double(area.getX(),hpa10133Ordinate),HPA_1013_3_COLOR));
            g2.fill(new Rectangle2D.Double(area.getX(),hpa10133Ordinate,area.getWidth(),area.getMaxY()-hpa10133Ordinate));
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),hpa10133Ordinate),HPA_1013_3_COLOR,new Point2D.Double(area.getX(),hpa1040Ordinate),HPA_1040_COLOR));
            g2.fill(new Rectangle2D.Double(area.getX(),area.getMinY(),area.getWidth(),hpa10133Ordinate-area.getMinY()));
        }
    }

    private static class PressureAxis extends NumberAxis
    {
        private PressureAxis()
        {
            setAutoRange(false);
        }

        @Override
        protected List refreshTicksVertical(Graphics2D g2,Rectangle2D dataArea,RectangleEdge edge)
        {
            ArrayList<NumberTick> ticks=new ArrayList<NumberTick>();
            for(double pressure=900d;pressure<=1100d;pressure+=1d)
                if(pressure%10d==0d)
                    ticks.add(new NumberTick(TickType.MAJOR,pressure,(int)pressure+" hPa",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
                else
                    ticks.add(new NumberTick(TickType.MINOR,pressure,"",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
            return ticks;
        }
    }

    /*
     *
     * CO2.
     *
     */

    private static class CO2Plot extends XYPlot
    {
        private static final Color PPM_400_COLOR=new Color(192,255,192);
        private static final Color PPM_1000_COLOR=new Color(255,224,192);
        private static final Color PPM_2000_COLOR=new Color(255,192,192);

        private CO2Plot(XYDataset dataset,ValueAxis domainAxis,ValueAxis rangeAxis,XYItemRenderer renderer)
        {
            super(dataset,domainAxis,rangeAxis,renderer);
            setDomainGridlinePaint(Color.BLACK);
            setRangeGridlinePaint(Color.BLACK);
            setRangeMinorGridlinesVisible(true);
            setRangeMinorGridlinePaint(Color.GRAY);
        }

        @Override
        protected void fillBackground(Graphics2D g2,Rectangle2D area,PlotOrientation orientation)
        {
            ValueAxis rangeAxis=getRangeAxis();
            double ppm0Ordinate=rangeAxis.valueToJava2D(0d,area,RectangleEdge.LEFT);
            double ppm400Ordinate=rangeAxis.valueToJava2D(400d,area,RectangleEdge.LEFT);
            double ppm1000Ordinate=rangeAxis.valueToJava2D(1000d,area,RectangleEdge.LEFT);
            double ppm2000Ordinate=rangeAxis.valueToJava2D(2000d,area,RectangleEdge.LEFT);
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),ppm400Ordinate),PPM_400_COLOR,new Point2D.Double(area.getX(),ppm1000Ordinate),PPM_1000_COLOR));
            g2.fill(new Rectangle2D.Double(area.getX(),ppm1000Ordinate,area.getWidth(),ppm0Ordinate-ppm1000Ordinate));
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),ppm1000Ordinate),PPM_1000_COLOR,new Point2D.Double(area.getX(),ppm2000Ordinate),PPM_2000_COLOR));
            g2.fill(new Rectangle2D.Double(area.getX(),area.getMinY(),area.getWidth(),ppm1000Ordinate-area.getMinY()));
        }
    }

    private static class CO2Axis extends NumberAxis
    {
        private CO2Axis()
        {
            setAutoRange(false);
        }

        @Override
        protected List refreshTicksVertical(Graphics2D g2,Rectangle2D dataArea,RectangleEdge edge)
        {
            ArrayList<NumberTick> ticks=new ArrayList<NumberTick>();
            for(double co2=0d;co2<=5000d;co2+=50d)
                if(co2%100d==0d)
                    ticks.add(new NumberTick(TickType.MAJOR,co2,(int)co2+" ppm",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
                else
                    ticks.add(new NumberTick(TickType.MINOR,co2,"",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
            return ticks;
        }
    }

    /*
     *
     * Bruit.
     *
     */

    private static class NoisePlot extends XYPlot
    {
        private static final Color DB_40_COLOR=new Color(192,255,192);
        private static final Color DB_70_COLOR=new Color(255,192,192);

        private NoisePlot(XYDataset dataset,ValueAxis domainAxis,ValueAxis rangeAxis,XYItemRenderer renderer)
        {
            super(dataset,domainAxis,rangeAxis,renderer);
            setDomainGridlinePaint(Color.BLACK);
            setRangeGridlinePaint(Color.BLACK);
            setRangeMinorGridlinesVisible(true);
            setRangeMinorGridlinePaint(Color.GRAY);
        }

        @Override
        protected void fillBackground(Graphics2D g2,Rectangle2D area,PlotOrientation orientation)
        {
            ValueAxis rangeAxis=getRangeAxis();
            double db40Ordinate=rangeAxis.valueToJava2D(40d,area,RectangleEdge.LEFT);
            double db70Ordinate=rangeAxis.valueToJava2D(70d,area,RectangleEdge.LEFT);
            g2.setPaint(new GradientPaint(new Point2D.Double(area.getX(),db40Ordinate),DB_40_COLOR,new Point2D.Double(area.getX(),db70Ordinate),DB_70_COLOR));
            g2.fill(area);
        }
    }

    private static class NoiseAxis extends NumberAxis
    {
        private NoiseAxis()
        {
            setAutoRange(false);
        }

        @Override
        protected List refreshTicksVertical(Graphics2D g2,Rectangle2D dataArea,RectangleEdge edge)
        {
            ArrayList<NumberTick> ticks=new ArrayList<NumberTick>();
            for(double noise=0d;noise<=200d;noise+=1d)
                if(noise%10d==0d)
                    ticks.add(new NumberTick(TickType.MAJOR,noise,(int)noise+" dB",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
                else
                    ticks.add(new NumberTick(TickType.MINOR,noise,"",TextAnchor.CENTER_RIGHT,TextAnchor.CENTER_RIGHT,0d));
            return ticks;
        }
    }
}

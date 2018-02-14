package sky.netatmo.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import sky.netatmo.Measure;
import sky.netatmo.MeasurementScale;
import sky.netatmo.MeasurementType;
import sky.netatmo.Measurer;

public class MeasureDatabase
{
    private final Measurer[] measurers;
    private final ArrayList<Range> ranges;
    private final Object lockObject;

    private static final File DATABASE_FILE=new File("database.xml");

    public MeasureDatabase(Measurer[] measurers)
    {
        lockObject=new Object();
        this.measurers=measurers;
        ranges=new ArrayList<Range>();
        load();
    }

    public Measure[] getMeasures(Measurer measurer,MeasurementScale measurementScale,Date beginDate,Date endDate,MeasurementType... measurementTypes)
    {
        synchronized(lockObject)
        {
            int storedRangeCount=ranges.size();
            int totalStoredMeasureCount=getTotalStoredMeasureCount();
            ArrayList<Measure> allMeasures=new ArrayList<Measure>();
            for(MeasurementType measurementType:measurementTypes)
            {
                ArrayList<Measure> compiledMeasures=new ArrayList<Measure>();
                ArrayList<Range> interestingRanges=new ArrayList<Range>();
                for(Range range:ranges)
                    if(range.measurer==measurer&&range.measurementScale==measurementScale&&range.measurementType==measurementType)
                        if(range.containsDate(beginDate)//la range courante entoure la date de début demandée
                           ||range.containsDate(endDate)//la range courante entoure la date de fin demandée
                           ||range.isCompletelyContainedByRange(beginDate,endDate)//la range courante est incluse dans la portion demandée
                           ||range.isPartiallyContainedByRange(beginDate,endDate)//la range courante est é cheval sur la portion demandée (é priori ce cas est déjé pris en compte par les tests précédents)
                                )
                            interestingRanges.add(range);
                Collections.sort(interestingRanges);//on va les trier par ordre chronologique sachant qu'elles ne se recouvrent jamais en théorie
                Date currentDate=beginDate;
                if(!interestingRanges.isEmpty()&&interestingRanges.get(0).beginDate.getTime()<beginDate.getTime())
                    currentDate=interestingRanges.get(0).beginDate;
                Date absoluteBeginDate=currentDate;
                int nextRangeIndex=0;
                while(currentDate.getTime()<endDate.getTime())//alimentation des mesures dont on a besoin
                    if(nextRangeIndex==interestingRanges.size()||currentDate.getTime()<interestingRanges.get(nextRangeIndex).beginDate.getTime())//si la range est plus loin, on va faire une requéte é distance pour alimenter la liste
                    {
                        ArrayList<Measure> returnedMeasures=new ArrayList<Measure>();
                        Date readToDate;
                        if(nextRangeIndex==interestingRanges.size())//s'il n'y a plus de range é lire par la suite, on spécifie la date de fin demandée
                            readToDate=new Date(endDate.getTime());
                        else//sinon, alors on s'arréte de lire jusqu'é la prochaine range é lire
                            readToDate=interestingRanges.get(nextRangeIndex).beginDate;
                        try
                        {
                            Measure[] array=measurer.getMeasures(MeasurementScale.MAX,currentDate,readToDate,measurementType);
                            for(Measure measure:array)
                                returnedMeasures.add(measure);
                            while(array.length==1024)
                                for(Measure measure:measurer.getMeasures(MeasurementScale.MAX,new Date(returnedMeasures.get(returnedMeasures.size()-1).getDate().getTime()+300000L),readToDate,measurementType))
                                    returnedMeasures.add(measure);
                            if(!returnedMeasures.isEmpty())
                                System.out.println("        "+returnedMeasures.size()+" mesures lues en tout sur le serveur");
                        }
                        catch(Exception e)
                        {
                        }
                        compiledMeasures.addAll(returnedMeasures);
                        currentDate=readToDate;
                    }
                    else//sinon, alors la range peut alimenter la liste
                    {
                        for(Measure measure:interestingRanges.get(nextRangeIndex).measures)
                            compiledMeasures.add(measure);
                        currentDate=interestingRanges.get(nextRangeIndex++).endDate;
                    }
                Collections.sort(compiledMeasures);
                Date endDateToConsider;
                if(System.currentTimeMillis()-currentDate.getTime()<30L*60L*1000L||System.currentTimeMillis()-endDate.getTime()<30L*60L*1000L)
                    endDateToConsider=new Date(compiledMeasures.get(compiledMeasures.size()-1).getDate().getTime()+1000L);
                else
                    endDateToConsider=currentDate;
                Range newRange=new Range(measurer,measurementScale,absoluteBeginDate,endDateToConsider,measurementType,compiledMeasures);
                for(Range interestingRange:interestingRanges)
                    ranges.remove(interestingRange);
                ranges.add(newRange);//éa remplace toutes les petites ranges dont on s'est servi
                for(Measure measure:compiledMeasures.toArray(new Measure[compiledMeasures.size()]))//épuration pour enlever les mesures qui sont en dehors de la portion demandée
                    if(measure.getDate().getTime()<=beginDate.getTime()||measure.getDate().getTime()>endDate.getTime())
                        compiledMeasures.remove(measure);
                allMeasures.addAll(compiledMeasures);
            }
            int newStoredRangeCount=ranges.size();
            int newTotalStoredMeasureCount=getTotalStoredMeasureCount();
            if(newStoredRangeCount!=storedRangeCount||newTotalStoredMeasureCount!=totalStoredMeasureCount)
                save();
            return allMeasures.toArray(new Measure[allMeasures.size()]);
        }
    }

    public int getTotalStoredMeasureCount()
    {
        synchronized(lockObject)
        {
            int count=0;
            for(Range range:ranges)
                count+=range.measures.length;
            return count;
        }
    }

    @SuppressWarnings("unchecked")
    private void load()
    {
        synchronized(DATABASE_FILE)
        {
            try
            {
                if(!DATABASE_FILE.exists())
                    return;
                Element databaseElement=new SAXBuilder().build(DATABASE_FILE).getRootElement();
                ranges.clear();
                for(Element rangeElement:databaseElement.getChildren("range"))
                {
                    Measurer measurer=findMeasurer(rangeElement.getAttributeValue("measurerName"),rangeElement.getAttributeValue("measurerId"));
                        if(measurer==null)
                            continue;
                    MeasurementScale measurementScale=MeasurementScale.valueOf(rangeElement.getAttributeValue("measurementScale"));
                    if(measurementScale==null)
                        continue;//ne devrait jamais arriver
                    Date beginDate=new Date(Long.parseLong(rangeElement.getAttributeValue("beginDate")));
                    Date endDate=new Date(Long.parseLong(rangeElement.getAttributeValue("endDate")));
                    MeasurementType measurementType=MeasurementType.valueOf(rangeElement.getAttributeValue("measurementType"));
                    if(measurementType==null)
                        continue;//ne devrait jamais arriver
                    ArrayList<Measure> measures=new ArrayList<Measure>();
                    for(Element measureElement:rangeElement.getChildren("measure"))
                    {
                        Date date=new Date(Long.parseLong(measureElement.getAttributeValue("date")));
                        double value=Double.parseDouble(measureElement.getAttributeValue("value"));
                        measures.add(new DatabaseMeasure(measurer,date,measurementType,value));
                    }
                    Range range=new Range(measurer,measurementScale,beginDate,endDate,measurementType,measures);
                    ranges.add(range);
                }
            }
            catch(Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    private void save()
    {
        synchronized(DATABASE_FILE)
        {
            try
            {
                Element databaseElement=new Element("database");
                for(Range range:ranges)
                {
                    Element rangeElement=new Element("range");
                    rangeElement.setAttribute("measurerName",range.measurer.getName());
                    rangeElement.setAttribute("measurerId",range.measurer.getId());
                    rangeElement.setAttribute("measurementScale",range.measurementScale.name());
                    rangeElement.setAttribute("beginDate",""+range.beginDate.getTime());
                    rangeElement.setAttribute("endDate",""+range.endDate.getTime());
                    rangeElement.setAttribute("measurementType",range.measurementType.name());
                    for(Measure measure:range.measures)
                    {
                        Element measureElement=new Element("measure");
                        measureElement.setAttribute("measurerName",measure.getMeasurer().getName());
                        measureElement.setAttribute("measurerId",measure.getMeasurer().getId());
                        measureElement.setAttribute("date",""+measure.getDate().getTime());
                        measureElement.setAttribute("measurementType",measure.getMeasurementType().name());
                        measureElement.setAttribute("value",""+measure.getValue());
                        rangeElement.addContent(measureElement);
                    }
                    databaseElement.addContent(rangeElement);
                }
                if(!DATABASE_FILE.exists())
                    DATABASE_FILE.createNewFile();
                else
                    DATABASE_FILE.delete();
                new XMLOutputter(Format.getPrettyFormat().setIndent("    ")).output(new Document(databaseElement),new FileOutputStream(DATABASE_FILE));
            }
            catch(Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    private Measurer findMeasurer(String name,String id)
    {
        for(Measurer measurer:measurers)
            if(measurer.getName().equals(name)&&measurer.getId().equals(id))
                return measurer;
        return null;
    }

    private static class Range implements Comparable<Range>
    {
        private final Measurer measurer;
        private final MeasurementScale measurementScale;
        private final Date beginDate;
        private final Date endDate;
        private final MeasurementType measurementType;
        private Measure[] measures;
        private boolean confirmed;

        private Range(Measurer measurer,MeasurementScale measurementScale,Date beginDate,Date endDate,MeasurementType measurementType,ArrayList<Measure> measures)
        {
            this.measurer=measurer;
            this.measurementScale=measurementScale;
            this.beginDate=beginDate;
            this.endDate=endDate;
            this.measurementType=measurementType;
            this.measures=measures.toArray(new Measure[measures.size()]);
            confirmed=false;
        }

        public int compareTo(Range anotherRange)
        {
            return beginDate.compareTo(anotherRange.beginDate);
        }

        private boolean containsDate(Date date)
        {
            return date.getTime()>=beginDate.getTime()&&date.getTime()<=endDate.getTime();
        }

        private boolean containsRange(Date beginDate,Date endDate)
        {
            return containsDate(beginDate)&&containsDate(endDate);
        }

        private boolean containsRange(Range range)
        {
            return containsRange(range.beginDate,range.endDate);
        }

        private boolean isCompletelyContainedByRange(Date beginDate,Date endDate)
        {
            return beginDate.getTime()<=this.beginDate.getTime()&&endDate.getTime()>=this.endDate.getTime();
        }

        private boolean isCompletelyContainedByRange(Range range)
        {
            return range.containsDate(beginDate)&&range.containsDate(endDate);
        }

        private boolean isPartiallyContainedByRange(Date beginDate,Date endDate)
        {
            return containsDate(beginDate)&&beginDate.getTime()<=this.endDate.getTime()&&endDate.getTime()>=this.endDate.getTime()||containsDate(endDate)&&beginDate.getTime()<=this.beginDate.getTime()&&endDate.getTime()>=this.beginDate.getTime();
        }

        private boolean isPartiallyContainedByRange(Range range)
        {
            return containsDate(range.beginDate)&&range.containsDate(endDate)||containsDate(range.endDate)&&range.containsDate(beginDate);
        }

        private Measure[] getMeasures()
        {
            return measures;
        }

        @Override
        public String toString()
        {
            String s="measurer="+measurer.getName();
            s+=", measurementScale="+measurementScale.name();
            s+=", beginDate="+SimpleDateFormat.getDateTimeInstance().format(beginDate);
            s+=", endDate="+SimpleDateFormat.getDateTimeInstance().format(endDate);
            s+=", measurementType="+measurementType.name();
            s+="/n{/n";
            for(Measure measure:measures)
                s+=measure.toString()+"/n";
            s+="}";
            return s;
        }
    }

    private static class DatabaseMeasure extends Measure
    {
        private DatabaseMeasure(Measurer measurer,Date date,MeasurementType measurementType,double value)
        {
            super(measurer,date,measurementType,value);
        }

        @Override
        public String toString()
        {
            return "    "+super.toString();
        }
    }
}

package sky.netatmo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DefaultMeasure implements Measure
{
    private final Measurer measurer;
    private final Date date;
    private final MeasurementType measurementType;
    private final double value;

    protected DefaultMeasure(Measurer measurer,Date date,MeasurementType measurementType,double value)
    {
        this.measurer=measurer;
        this.date=date;
        this.measurementType=measurementType;
        this.value=value;
    }

    public Measurer getMeasurer()
    {
        return measurer;
    }

    public Date getDate()
    {
        return date;
    }

    public MeasurementType getMeasurementType()
    {
        return measurementType;
    }

    public double getValue()
    {
        return value;
    }

    public int compareTo(Measure anotherMeasure)
    {
        return date.compareTo(anotherMeasure.getDate());
    }

    @Override
    public String toString()
    {
        return measurer.getName()+" @ "+SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,SimpleDateFormat.MEDIUM,measurer.getUser().getLocale()).format(date)+" : "+measurementType.getInputString()+"="+(Double.isNaN(value)?"N/A":(value+measurementType.getUnitString()));
    }

    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof Measure))
            return false;
        Measure anotherMeasure=(Measure)o;
        if(measurer!=anotherMeasure.getMeasurer())
            return false;
        if(!date.equals(anotherMeasure.getDate()))
            return false;
        if(measurementType!=anotherMeasure.getMeasurementType())
            return false;
        return value==anotherMeasure.getValue();
    }

    public static Measure[] filter(Measure[] measures,MeasurementType measurementType)
    {
        ArrayList<Measure> list=new ArrayList<>();
        for(Measure measure:measures)
            if(measure.getMeasurementType()==measurementType)
                list.add(measure);
        return list.toArray(new Measure[list.size()]);
    }
}

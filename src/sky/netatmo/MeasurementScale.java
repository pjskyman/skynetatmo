package sky.netatmo;

public enum MeasurementScale
{
    MAX
    {
        String getInputString()
        {
            return "max";
        }
    },
    _30_MINUTES
    {
        String getInputString()
        {
            return "30min";
        }
    },
    _3_HOURS
    {
        String getInputString()
        {
            return "3hours";
        }
    },
    _1_DAY
    {
        String getInputString()
        {
            return "1day";
        }
    },
    _1_WEEK
    {
        String getInputString()
        {
            return "1week";
        }
    },
    _1_MONTH
    {
        String getInputString()
        {
            return "1month";
        }
    };

    abstract String getInputString();
}

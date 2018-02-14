package sky.netatmo;

public enum MeasurementType
{
    TEMPERATURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return true;
        }

        String getInputString()
        {
            return "Temperature";
        }

        String getUnitString()
        {
            return "°C";
        }

        String getJSONCode()
        {
            return "a";
        }
    },
    HUMIDITY
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return true;
        }

        String getInputString()
        {
            return "Humidity";
        }

        String getUnitString()
        {
            return "%";
        }

        String getJSONCode()
        {
            return "b";
        }
    },
    PRESSURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return true;
        }

        String getInputString()
        {
            return "Pressure";
        }

        String getUnitString()
        {
            return " hPa";
        }

        String getJSONCode()
        {
            return "'";
        }
    },
    CO2
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return true;
        }

        String getInputString()
        {
            return "CO2";
        }

        String getUnitString()
        {
            return " ppm";
        }

        String getJSONCode()
        {
            return "h";
        }
    },
    NOISE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return true;
        }

        String getInputString()
        {
            return "Noise";
        }

        String getUnitString()
        {
            return " dB";
        }

        String getJSONCode()
        {
            return "S";
        }
    },
    MIN_TEMPERATURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "min_temp";
        }

        String getUnitString()
        {
            return "°C";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MAX_TEMPERATURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "max_temp";
        }

        String getUnitString()
        {
            return "°C";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MIN_HUMIDITY
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "min_hum";
        }

        String getUnitString()
        {
            return "%";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MAX_HUMIDITY
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "max_hum";
        }

        String getUnitString()
        {
            return "%";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MIN_PRESSURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "min_pressure";
        }

        String getUnitString()
        {
            return " hPa";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MAX_PRESSURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "max_pressure";
        }

        String getUnitString()
        {
            return " hPa";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MIN_NOISE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "min_noise";
        }

        String getUnitString()
        {
            return " dB";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    MAX_NOISE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale!=MeasurementScale.MAX;
        }

        String getInputString()
        {
            return "max_noise";
        }

        String getUnitString()
        {
            return " dB";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MIN_TEMPERATURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_min_temp";
        }

        String getUnitString()
        {
            return "°C";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MAX_TEMPERATURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_max_temp";
        }

        String getUnitString()
        {
            return "°C";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MIN_HUMIDITY
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_min_hum";
        }

        String getUnitString()
        {
            return "%";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MAX_HUMIDITY
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_max_hum";
        }

        String getUnitString()
        {
            return "%";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MIN_PRESSURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_min_pressure";
        }

        String getUnitString()
        {
            return " hPa";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MAX_PRESSURE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_max_pressure";
        }

        String getUnitString()
        {
            return " hPa";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MIN_NOISE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_min_noise";
        }

        String getUnitString()
        {
            return " dB";
        }

        String getJSONCode()
        {
            return "";
        }
    },
    DATE_MAX_NOISE
    {
        boolean isCompatibleWith(MeasurementScale scale)
        {
            return scale==MeasurementScale._1_DAY||scale==MeasurementScale._1_WEEK||scale==MeasurementScale._1_MONTH;
        }

        String getInputString()
        {
            return "date_max_noise";
        }

        String getUnitString()
        {
            return " dB";
        }

        String getJSONCode()
        {
            return "";
        }
    };

    abstract boolean isCompatibleWith(MeasurementScale scale);

    abstract String getInputString();

    abstract String getUnitString();

    abstract String getJSONCode();
}

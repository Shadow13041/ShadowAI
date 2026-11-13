package com.shadowai.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AiStats {
    private AiStats(){}

    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences("ai_statistics",Context.MODE_PRIVATE);
    }

    public static synchronized String record(
            Context context,long milliseconds,int inputTokens,int outputTokens) {

        SharedPreferences p=prefs(context);
        long requests=p.getLong("requests",0)+1;
        long totalTime=p.getLong("total_time",0)+milliseconds;
        long input=p.getLong("input_tokens",0)+inputTokens;
        long output=p.getLong("output_tokens",0)+outputTokens;

        JSONArray history;
        try{
            history=new JSONArray(p.getString("history","[]"));
        }catch(Exception e){
            history=new JSONArray();
        }

        JSONObject entry=new JSONObject();
        try{
            entry.put("time",System.currentTimeMillis());
            entry.put("duration",milliseconds);
            entry.put("input",inputTokens);
            entry.put("output",outputTokens);
            history.put(entry);
        }catch(Exception ignored){}

        while(history.length()>30){
            JSONArray reduced=new JSONArray();
            for(int i=1;i<history.length();i++)
                reduced.put(history.opt(i));
            history=reduced;
        }

        p.edit()
            .putLong("requests",requests)
            .putLong("total_time",totalTime)
            .putLong("input_tokens",input)
            .putLong("output_tokens",output)
            .putString("history",history.toString())
            .apply();

        return String.format(Locale.GERMANY,
            "Antwortzeit: %.2f s · Tokens: %d rein / %d raus",
            milliseconds/1000.0,inputTokens,outputTokens);
    }

    public static String summary(Context context) {
        SharedPreferences p=prefs(context);
        long requests=p.getLong("requests",0);
        long time=p.getLong("total_time",0);
        long input=p.getLong("input_tokens",0);
        long output=p.getLong("output_tokens",0);

        StringBuilder result=new StringBuilder();
        result.append("KI-Gesamtstatistik\n\n");
        result.append("Anfragen: ").append(requests).append('\n');
        result.append(String.format(Locale.GERMANY,
            "Gesamte Antwortzeit: %.1f Sekunden\n",time/1000.0));
        result.append(String.format(Locale.GERMANY,
            "Durchschnitt: %.2f Sekunden\n",
            requests==0?0:(time/1000.0)/requests));
        result.append("Eingabetokens: ").append(input).append('\n');
        result.append("Ausgabetokens: ").append(output).append('\n');
        result.append("Gesamtverbrauch: ").append(input+output).append(" Tokens\n");

        result.append("\nLetzte Anfragen:\n");
        try{
            JSONArray history=new JSONArray(p.getString("history","[]"));
            SimpleDateFormat format=new SimpleDateFormat(
                "dd.MM. HH:mm",Locale.GERMANY);

            for(int i=history.length()-1;i>=0;i--){
                JSONObject e=history.getJSONObject(i);
                result.append("\n")
                    .append(format.format(new Date(e.optLong("time"))))
                    .append(" · ")
                    .append(String.format(Locale.GERMANY,
                        "%.2f s",e.optLong("duration")/1000.0))
                    .append("\n")
                    .append("Tokens: ")
                    .append(e.optInt("input"))
                    .append(" rein / ")
                    .append(e.optInt("output"))
                    .append(" raus\n");
            }
        }catch(Exception e){
            result.append("Verlauf konnte nicht geladen werden.\n");
        }
        return result.toString();
    }

    public static void reset(Context context) {
        prefs(context).edit().clear().apply();
    }
}

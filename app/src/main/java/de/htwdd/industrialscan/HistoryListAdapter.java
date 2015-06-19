package de.htwdd.industrialscan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Simon on 30.05.15.
 */
public class HistoryListAdapter extends ArrayAdapter<String>
{
    private final Context context;
    private final String[] values;

    public HistoryListAdapter(Context context, String[] values)
    {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.history_list_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.textView);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView2);
        textView.setText(values[position]);

        // Change icon based on name
        String s = values[position];

        if (s.contains("angemeldet"))
        {
            imageView.setImageResource(R.drawable.unlocked);
        }
        else if (s.contains("abgemeldet"))
        {
            imageView.setImageResource(R.drawable.locked);
        }
        return rowView;
    }
}
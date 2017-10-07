package fr.trainz.ppr.contactssaver;

/**
 * Created by PPR on 12/02/2017.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class FArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private boolean checked[];

    public FArrayAdapter(Context context, String[] values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.checked = new boolean[values.length];

        for(int i = 0; i < checked.length; i++)
            checked[i] = true;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item, parent, false);
        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.CBX_ITEM);
        checkBox.setText(values[position]);
        checkBox.setChecked(checked[position]);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checked[position] = isChecked;
            }
        });
        return rowView;
    }

    public void checkEverything(boolean check) {
        for(int i = 0; i < checked.length; i++)
            checked[i] = check;
    }

    public boolean[] getChecks() {
        return checked;
    }
}
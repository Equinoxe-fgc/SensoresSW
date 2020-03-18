package com.equinoxe.sensoressw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class MiAdaptadorSensores extends RecyclerView.Adapter<MiAdaptadorSensores.ViewHolderSensores> {
    private LayoutInflater inflador;
    private BluetoothServiceInfoList lista;

    public MiAdaptadorSensores(Context context, BluetoothServiceInfoList lista) {
        this.lista = lista;
        inflador = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public ViewHolderSensores onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflador.inflate(R.layout.elemento_lista_sensores, parent, false);
        return new ViewHolderSensores(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MiAdaptadorSensores.ViewHolderSensores holder, final int position) {
        BluetoothServiceInfo info = lista.getBluetoothServiceInfo(position);

        holder.chkSensorSelected.setChecked(info.isSelected());
        holder.txtSensorName.setText(info.getName());
        holder.txtSensorUUID.setText(info.getUUID());

        holder.chkSensorSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                lista.getBluetoothServiceInfo(position).setSelected(b);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.getSize();
    }

    public class ViewHolderSensores extends RecyclerView.ViewHolder {
        public CheckBox chkSensorSelected;
        public TextView txtSensorName;
        public TextView txtSensorUUID;

        public ViewHolderSensores(View itemView) {
            super(itemView);
            chkSensorSelected = itemView.findViewById(R.id.chkSelectSensor);
            txtSensorName = itemView.findViewById(R.id.txtSensorName);
            txtSensorUUID = itemView.findViewById(R.id.txtSensorUUID);
        }
    }
}

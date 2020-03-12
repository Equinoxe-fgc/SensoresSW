package com.equinoxe.sensoressw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class MiAdaptador extends RecyclerView.Adapter<MiAdaptador.ViewHolder> {
    private LayoutInflater inflador;
    private BluetoothDeviceInfoList lista;
    private MainActivity mainActivity;

    //private View.OnClickListener onClickListener;

    public MiAdaptador(Context context, BluetoothDeviceInfoList lista, MainActivity mainActivity) {
        this.lista = lista;
        this.mainActivity = mainActivity;
        inflador = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /*public void setOnItemClickListener(View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
    }*/

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflador.inflate(R.layout.elemento_lista, parent, false);
        //v.setOnClickListener(onClickListener);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MiAdaptador.ViewHolder holder, final int position) {
        BluetoothDeviceInfo info = lista.getBluetoothDeviceInfo(position);

        holder.chkSelected.setChecked(info.isSelected());
        holder.txtDescription.setText(info.getDescription());
        holder.txtDescription2.setText(info.getAddress());

        holder.btnConectOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.connectOne(position);
            }
        });
        holder.chkSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothDeviceInfo info = lista.getBluetoothDeviceInfo(position);
                info.setSelected(holder.chkSelected.isChecked());

                boolean bSomeSelected = false;
                for (int i = 0; i < lista.getSize(); i++) {
                    info = lista.getBluetoothDeviceInfo(i);
                    bSomeSelected |= info.isSelected();
                }
                mainActivity.notifySomeSelected(bSomeSelected);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.getSize();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CheckBox chkSelected;
        public TextView txtDescription, txtDescription2;
        public Button btnConectOne;

        public ViewHolder(View itemView) {
            super(itemView);
            chkSelected = itemView.findViewById(R.id.chkSelectSensorTag);
            txtDescription = itemView.findViewById(R.id.txtSensorTagName);
            txtDescription2 = itemView.findViewById(R.id.txtDescription);
            btnConectOne = itemView.findViewById(R.id.btnConnectOne);
        }
    }
}

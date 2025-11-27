package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView // Faltaba el 'import' aquí o estaba mal escrito

class SolicitudesAdapter(
    private val lista: List<UsuarioPublico>,
    private val onAceptar: (UsuarioPublico) -> Unit,
    private val onRechazar: (UsuarioPublico) -> Unit
) : RecyclerView.Adapter<SolicitudesAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvNombreSolicitud)
        val btnOk: ImageButton = v.findViewById(R.id.btnAceptar)
        val btnNo: ImageButton = v.findViewById(R.id.btnRechazar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud, parent, false)
        return VH(view)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val usuario = lista[position]
        holder.tv.text = usuario.nombre

        holder.btnOk.setOnClickListener { onAceptar(usuario) }
        holder.btnNo.setOnClickListener { onRechazar(usuario) }
    }
}
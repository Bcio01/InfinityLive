package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.saamael.infinitylive.databinding.ItemRutinaBinding
import java.util.Locale

class RutinasAdapter(
    private val listaRutinas: List<Rutina>,
    private val onBorrarClick: (Rutina) -> Unit,
    private val onSwitchChange: (Rutina, Boolean) -> Unit
) : RecyclerView.Adapter<RutinasAdapter.RutinaViewHolder>() {

    inner class RutinaViewHolder(val binding: ItemRutinaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutinaViewHolder {
        val binding = ItemRutinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RutinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RutinaViewHolder, position: Int) {
        val rutina = listaRutinas[position]

        // Formatear hora (ej: 07:05)
        holder.binding.tvHoraItem.text = String.format(Locale.getDefault(), "%02d:%02d", rutina.hora, rutina.minuto)
        holder.binding.tvTipoItem.text = "Modo: ${rutina.tipo}"

        // Configurar Switch sin disparar el listener accidentalmente
        holder.binding.switchItem.setOnCheckedChangeListener(null)
        holder.binding.switchItem.isChecked = rutina.activa

        holder.binding.switchItem.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChange(rutina, isChecked)
        }

        holder.binding.btnBorrarItem.setOnClickListener {
            onBorrarClick(rutina)
        }
    }

    override fun getItemCount() = listaRutinas.size
}
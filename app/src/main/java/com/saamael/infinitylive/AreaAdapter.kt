package com.saamael.infinitylive // Asegúrate de que este sea tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.saamael.infinitylive.databinding.ItemAreaBinding // Binding para item_area.xml
import java.text.DecimalFormat

// 1. La clase hereda de FirestoreRecyclerAdapter
class AreaAdapter(options: FirestoreRecyclerOptions<Area>) :
    FirestoreRecyclerAdapter<Area, AreaAdapter.AreaViewHolder>(options) {

    // 2. El ViewHolder (el "molde" para cada fila) usa ViewBinding
    inner class AreaViewHolder(private val binding: ItemAreaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 3. Función para "rellenar" cada fila con datos
        fun bind(area: Area) {
            binding.tvAreaNombre.text = area.nombre_area
            // Solo mostramos el número del nivel porque el texto "Nivel" ya está fijo en el XML
            binding.tvAreaNivel.text = area.nivel.toString()

            // Lógica para la barra de progreso de XP
            // Asumimos que la XP necesaria es Nivel * 100
            val xpNecesaria = area.nivel * 100
            if (xpNecesaria > 0) {
                // Convertimos a porcentaje
                val xpNecesaria = area.nivel * 100
                val porcentaje = if (xpNecesaria > 0) (area.xp.toInt() * 100) / xpNecesaria.toInt() else 0
                binding.pbAreaXp.max = 100
                binding.pbAreaXp.progress = porcentaje
            } else {
                // Por si acaso, para evitar división por cero
                binding.pbAreaXp.max = 100
                binding.pbAreaXp.progress = 0
            }
        }
    }

    // 4. Crea un nuevo ViewHolder cuando la lista lo necesita
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaViewHolder {
        val binding = ItemAreaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AreaViewHolder(binding)
    }

    // 5. Llama a la función bind() para rellenar la fila
    override fun onBindViewHolder(holder: AreaViewHolder, position: Int, model: Area) {
        // 'model' es el objeto Area de Firestore para esa fila
        holder.bind(model)
    }
}
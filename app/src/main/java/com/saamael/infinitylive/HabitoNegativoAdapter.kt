package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.saamael.infinitylive.databinding.ItemHabitoNegativoBinding // Binding para el item negativo

class HabitoNegativoAdapter(options: FirestoreRecyclerOptions<Habito>) :
    FirestoreRecyclerAdapter<Habito, HabitoNegativoAdapter.HabitoNegativoViewHolder>(options) {

    // TODO: Programaremos este listener en la rama "funcionalidad/logica-juego"
    // var onHabitoCometido: ((String) -> Unit)? = null

    inner class HabitoNegativoViewHolder(private val binding: ItemHabitoNegativoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habito: Habito) {
            binding.tvHabitoDescripcion.text = habito.descripcion
            binding.tvHabitoCastigo.text = "-${habito.hp_perdida} HP"

            // TODO: Programar el clic del botón
            binding.btnCometerHabito.setOnClickListener {
                // Aquí llamaremos a la función para restar HP
                // onHabitoCometido?.invoke(getItem(adapterPosition).id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitoNegativoViewHolder {
        val binding = ItemHabitoNegativoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitoNegativoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitoNegativoViewHolder, position: Int, model: Habito) {
        holder.bind(model)
    }
}
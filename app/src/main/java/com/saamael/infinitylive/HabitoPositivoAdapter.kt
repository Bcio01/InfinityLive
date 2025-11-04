package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.saamael.infinitylive.databinding.ItemHabitoPositivoBinding // Binding para el item positivo

class HabitoPositivoAdapter(options: FirestoreRecyclerOptions<Habito>) :
    FirestoreRecyclerAdapter<Habito, HabitoPositivoAdapter.HabitoPositivoViewHolder>(options) {

    // TODO: Programaremos este listener en la rama "funcionalidad/logica-juego"
    // var onHabitoCompletado: ((String) -> Unit)? = null

    inner class HabitoPositivoViewHolder(private val binding: ItemHabitoPositivoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habito: Habito) {
            binding.tvHabitoDescripcion.text = habito.descripcion

            // Creamos el texto de la recompensa
            // (En un futuro, podríamos buscar el nombre del área usando el 'area_id')
            binding.tvHabitoRecompensa.text = "+${habito.xp_ganada} XP | +${habito.monedas_ganadas} Monedas"

            // TODO: Programar el clic del botón
            binding.btnCompletarHabito.setOnClickListener {
                // Aquí llamaremos a la función para sumar XP y monedas
                // onHabitoCompletado?.invoke(getItem(adapterPosition).id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitoPositivoViewHolder {
        val binding = ItemHabitoPositivoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitoPositivoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitoPositivoViewHolder, position: Int, model: Habito) {
        holder.bind(model)
    }
}
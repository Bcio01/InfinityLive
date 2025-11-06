package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.saamael.infinitylive.databinding.ItemHabitoPositivoBinding // Binding para el item positivo

class HabitoPositivoAdapter(options: FirestoreRecyclerOptions<Habito>) :
    FirestoreRecyclerAdapter<Habito, HabitoPositivoAdapter.HabitoPositivoViewHolder>(options) {

    // --- AÑADE ESTA INTERFAZ ---
    // Esta es la "línea telefónica" para hablar con InicioActivity
    interface OnHabitoPositivoListener {
        fun onHabitoCompletado(habito: Habito)
    }
    // ---

    // --- AÑADE ESTA VARIABLE ---
    var listener: OnHabitoPositivoListener? = null
    // ---

    inner class HabitoPositivoViewHolder(private val binding: ItemHabitoPositivoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habito: Habito) {
            binding.tvHabitoDescripcion.text = habito.descripcion
            binding.tvHabitoRecompensa.text = "+${habito.xp_ganada} XP | +${habito.monedas_ganadas} Monedas"

            // --- AHORA PROGRAMAMOS EL CLIC ---
            binding.btnCompletarHabito.setOnClickListener {
                // Llama a la "línea telefónica" y pasa el hábito
                listener?.onHabitoCompletado(getItem(adapterPosition))
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
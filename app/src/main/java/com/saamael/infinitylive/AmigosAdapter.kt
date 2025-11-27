package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AmigosAdapter(
    private val listaAmigos: List<UsuarioPublico>,
    private val onItemClick: (UsuarioPublico) -> Unit
) : RecyclerView.Adapter<AmigosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // CORRECCIÓN: Usamos los IDs que SI existen en tu nuevo item_amigo.xml
        val tvNombre: TextView = view.findViewById(R.id.tvNombreAmigo)
        val tvNivel: TextView = view.findViewById(R.id.tvNivelGlobal) // Antes era tvNivelAmigo
        val tvAreas: TextView = view.findViewById(R.id.tvResumenAreas) // Nuevo campo de texto
        // Hemos eliminado 'btnVerPerfil' porque lo borraste del XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_amigo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val amigo = listaAmigos[position]

        // 1. Asignar Nombre
        holder.tvNombre.text = amigo.nombre

        // 2. Asignar Nivel Global
        holder.tvNivel.text = "Nivel Global: ${amigo.nivelGlobal}"

        // 3. Asignar Resumen de Áreas (Formateado bonito)
        if (amigo.areasResumen.isNotEmpty()) {
            // Convierte el mapa {Fuerza=5, Inteligencia=3} a "Fuerza: 5  •  Inteligencia: 3"
            val textoAreas = amigo.areasResumen.entries.joinToString("  •  ") {
                "${it.key}: ${it.value}"
            }
            holder.tvAreas.text = textoAreas
        } else {
            holder.tvAreas.text = "Sin progreso registrado"
        }

        // 4. Click en toda la tarjeta (en lugar del botón "Ver")
        holder.itemView.setOnClickListener {
            onItemClick(amigo)
        }
    }

    override fun getItemCount() = listaAmigos.size
}
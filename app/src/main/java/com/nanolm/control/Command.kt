package com.nanolm.control

import java.text.Normalizer

sealed interface Command {
 data class OpenApp(val name: String) : Command
 data class Search(val query: String) : Command
 data class TypeText(val text: String) : Command
 data object Back : Command
 data object Home : Command
 data class Unknown(val original: String) : Command
}

object CommandParser {
 private fun normalize(s: String): String = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
 fun parse(raw: String): Command {
  val text = raw.trim()
  val n = normalize(text)
  if (n == "voltar" || n == "volta" || n == "vai para tras") return Command.Back
  if (n == "inicio" || n == "tela inicial" || n == "ir para tela inicial") return Command.Home
  val search = Regex("^(?:pesquise|pesquisa|buscar|busque|procure) (?:por |sobre )?(.+)$", RegexOption.IGNORE_CASE).find(text)
  if (search != null) return Command.Search(search.groupValues[1])
  val open = Regex("^(?:abra|abre|abrir) (?:o |a |app |aplicativo )*(.+)$", RegexOption.IGNORE_CASE).find(n)
  if (open != null) return Command.OpenApp(open.groupValues[1])
  val type = Regex("^(?:escreva|escreve|digite|digita) (.+)$", RegexOption.IGNORE_CASE).find(text)
  if (type != null) return Command.TypeText(type.groupValues[1])
  return Command.Unknown(text)
 }
 fun describe(c: Command): String = when(c) {
  is Command.OpenApp -> "Abrir aplicativo: ${c.name}"
  is Command.Search -> "Pesquisar no navegador: ${c.query}"
  is Command.TypeText -> "Escrever no campo em foco: ${c.text}"
  Command.Back -> "Voltar uma tela"
  Command.Home -> "Ir para a tela inicial"
  is Command.Unknown -> "Comando não reconhecido: ${c.original}"
 }
}

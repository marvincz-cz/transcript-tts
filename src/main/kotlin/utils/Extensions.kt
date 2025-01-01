package cz.marvincz.transcript.tts.utils

fun <T> MutableList<T>.replaceAllIndexed(operator: (Int, T) -> T) {
    val li = listIterator()
    while (li.hasNext()) {
        li.set(operator(li.nextIndex(), li.next()))
    }
}
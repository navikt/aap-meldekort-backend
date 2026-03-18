
class TimerArbeidetRepositoryFake: TimerArbeidetRepository {
    override fun hentTimerArbeidet(ident: Ident, sak: FagsakReferanse, periode: Periode): List<TimerArbeidet> {
        return listOf()
    }

    override fun lagreTimerArbeidet(ident: Ident, opplysninger: List<TimerArbeidet>) {
    }

    override fun hentSenesteOpplysningsdato(ident: Ident, fagsak: FagsakReferanse): LocalDate? {
        return null
    }
}
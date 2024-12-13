package no.nav.aap.meldekort.arena

//backend
private fun hentEtterregistrerteMeldekort(meldekortListe: List<Meldekort>) = meldekortListe
    .filter { it.kortType == KortType.MANUELL_ARENA }
    .filter { it.kortStatus == KortStatus.OPPRE }

private fun hentVanligeMeldekort(meldekortListe: List<Meldekort>) = meldekortListe
    .filter { it.kortType != KortType.MANUELL_ARENA }

private fun erKorrigerbart(meldekort: OrdsMedlekort, meldekortListe: List<OrdsMedlekort>): Boolean {
    if (meldekort.kortType == KORRIGERT_ELEKTRONISK || KortStatus.valueOf(meldekort.beregningstatus) == KortStatus.UBEHA) {
        return false
    } else {
        if (meldekortListe.find { mk ->
                (meldekort.meldekortId != mk.meldekortId && meldekort.meldeperiode == mk.meldeperiode && mk.kortType == "10")
            } == null) {
            return true
        }
        return false
    }
}

//frontend
const etterregistrerte = person?.etterregistrerteMeldekort
.filter(meldekort => meldekort.kortStatus === KortStatus.OPPRE || meldekort.kortStatus === KortStatus.SENDT)
.filter(meldekort => meldekort.meldeperiode.kanKortSendes)
.sort(meldekortEtterKanSendesFraKomparator)



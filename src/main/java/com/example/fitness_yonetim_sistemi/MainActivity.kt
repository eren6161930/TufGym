package com.example.fitness_yonetim_sistemi

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

// Tema Renkleri
val MaviAnaRenk = Color(0xFF1976D2)
val MaviAcikRenk = Color(0xFFBBDEFB)
val MaviKoyuRenk = Color(0xFF0D47A1)

enum class Rol { SECILMEDI, CEO, ANTRENOR, UYE }

// Firebase Firestore referansı (tek sefer oluşturulur)
private val db = Firebase.firestore

// Sabit Listeler (Composable dışında tanımlanır, her recomposition'da yeniden oluşturulmaz)
private val GUNLER = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
private val KATEGORILER = listOf("Göğüs", "Sırt", "Omuz", "Kol", "Bacak", "Karın", "Kardiyo")
private val HAREKETLER_MAP = mapOf(
    "Göğüs" to listOf("Bench Press", "Incline Press", "Chest Fly", "Push Up", "Dips"),
    "Sırt" to listOf("Lat Pulldown", "Seated Row", "Pull Up", "Deadlift", "Bent Over Row"),
    "Omuz" to listOf("Shoulder Press", "Lateral Raise", "Front Raise", "Face Pull", "Shrug"),
    "Kol" to listOf("Biceps Curl", "Triceps Pushdown", "Hammer Curl", "Skull Crusher"),
    "Bacak" to listOf("Squat", "Leg Press", "Leg Extension", "Leg Curl", "Lunge"),
    "Karın" to listOf("Plank", "Crunch", "Leg Raise", "Russian Twist"),
    "Kardiyo" to listOf("Koşu", "Bisiklet", "Yüzme", "İp Atlama", "HIIT")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = MaviAnaRenk,
                    secondary = MaviKoyuRenk,
                    tertiary = MaviAcikRenk
                )
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AnaEkranYonlendirici()
                }
            }
        }
    }
}

// Türkçe karakterleri İngilizce karşılıklarına çeviren ve formatlayan yardımcı fonksiyon
fun String.turkceKarakterSadelestir(): String {
    var sonuc = this.lowercase()
    val degisimler = mapOf(
        'ş' to 's', 'ı' to 'i', 'ğ' to 'g', 'ü' to 'u', 'ö' to 'o', 'ç' to 'c'
    )
    degisimler.forEach { (eski, yeni) ->
        sonuc = sonuc.replace(eski, yeni)
    }
    return sonuc.replace(" ", "")
}

@Composable
fun AnaEkranYonlendirici() {
    val aktifRolState = remember { mutableStateOf(Rol.SECILMEDI) }
    val girisYapildiState = remember { mutableStateOf(false) }
    val girisYapanKullaniciAdiState = remember { mutableStateOf("") }

    val aktifRol = aktifRolState.value
    val girisYapildi = girisYapildiState.value
    val girisYapanKullaniciAdi = girisYapanKullaniciAdiState.value

    when {
        aktifRol == Rol.SECILMEDI -> {
            RolSecimEkrani(onRolSecildi = { aktifRolState.value = it })
        }
        !girisYapildi -> {
            GirisEkrani(
                rol = aktifRol,
                onGirisBasarili = { kadi -> 
                    girisYapanKullaniciAdiState.value = kadi
                    girisYapildiState.value = true 
                },
                onGeri = { aktifRolState.value = Rol.SECILMEDI }
            )
        }
        else -> {
            when (aktifRol) {
                Rol.CEO -> CeoPanel(geriDon = {
                    girisYapildiState.value = false
                    aktifRolState.value = Rol.SECILMEDI
                })
                Rol.ANTRENOR -> AntrenorPaneli(
                    kullaniciAdi = girisYapanKullaniciAdi,
                    geriDon = {
                        girisYapildiState.value = false
                        aktifRolState.value = Rol.SECILMEDI
                    }
                )
                Rol.UYE -> UyePaneli(
                    kullaniciAdi = girisYapanKullaniciAdi,
                    geriDon = {
                        girisYapildiState.value = false
                        aktifRolState.value = Rol.SECILMEDI
                    }
                )
                else -> {}
            }
        }
    }
}

@Composable
fun RolSecimEkrani(onRolSecildi: (Rol) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TUF GYM", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaviAnaRenk)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Hoş geldiniz, lütfen rolünüzü seçin", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(48.dp))

        PanelButonu("CEO Girişi", MaviKoyuRenk) { onRolSecildi(Rol.CEO) }
        Spacer(modifier = Modifier.height(16.dp))
        PanelButonu("Antrenör Girişi", MaviAnaRenk) { onRolSecildi(Rol.ANTRENOR) }
        Spacer(modifier = Modifier.height(16.dp))
        PanelButonu("Üye Girişi", MaviAcikRenk, Color.Black) { onRolSecildi(Rol.UYE) }
    }
}

@Composable
fun GirisEkrani(rol: Rol, onGirisBasarili: (String) -> Unit, onGeri: () -> Unit) {
    var kullaniciAdi by remember { mutableStateOf("") }
    var sifre by remember { mutableStateOf("") }
    var yukleniyor by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${rol.name} Girişi", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaviAnaRenk)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = kullaniciAdi,
            onValueChange = { kullaniciAdi = it },
            label = { Text("Kullanıcı Adı (AdSoyad)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sifre,
            onValueChange = { sifre = it },
            label = { Text("Şifre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (yukleniyor) {
            CircularProgressIndicator(color = MaviAnaRenk)
        } else {
            Button(
                onClick = {
                    if (kullaniciAdi.isEmpty() || sifre.isEmpty()) {
                        Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    yukleniyor = true
                    if (rol == Rol.CEO) {
                        if (kullaniciAdi == "erenturan" && sifre == "12345678") {
                            val ceoVeri = hashMapOf("ad" to "Eren Turan", "rol" to "CEO", "kullaniciAdi" to "erenturan")
                            db.collection("Kullanicilar").document("erenturan").set(ceoVeri)
                                .addOnCompleteListener {
                                    yukleniyor = false
                                    onGirisBasarili("erenturan")
                                }
                        } else {
                            yukleniyor = false
                            Toast.makeText(context, "Hatalı CEO Kullanıcı Adı veya Şifre!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Türkçe karakter desteği ve küçük harfe çevirme
                        val arananKullanici = kullaniciAdi.turkceKarakterSadelestir()
                        db.collection("Kullanicilar")
                            .whereEqualTo("kullaniciAdi", arananKullanici)
                            .whereEqualTo("sifre", sifre)
                            .whereEqualTo("rol", rol.name)
                            .get()
                            .addOnSuccessListener { documents ->
                                yukleniyor = false
                                if (!documents.isEmpty) {
                                    onGirisBasarili(arananKullanici)
                                } else {
                                    Toast.makeText(context, "Kullanıcı adı veya şifre hatalı!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                yukleniyor = false
                                Toast.makeText(context, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Giriş Yap")
            }
        }

        TextButton(onClick = onGeri) {
            Text("Geri Dön", color = Color.Gray)
        }
    }
}

@Composable
fun PanelButonu(metin: String, renk: Color, metinRengi: Color = Color.White, tiklama: () -> Unit) {
    Button(
        onClick = tiklama,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = renk, contentColor = metinRengi)
    ) {
        Text(metin, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CeoPanel(geriDon: () -> Unit) {
    val context = LocalContext.current
    var yeniKullaniciAdSoyad by remember { mutableStateOf("") }
    var baslangicTarihi by remember { mutableStateOf("01.01.2026") }
    var bitisTarihi by remember { mutableStateOf("01.04.2026") }
    var ucret by remember { mutableStateOf("") }
    var secilenKayitRolu by remember { mutableStateOf(Rol.UYE) }
    var secilenAntrenor by remember { mutableStateOf("Antrenör Seçilmedi") }
    var yukleniyor by remember { mutableStateOf(false) }
    var antrenorMenuAcik by remember { mutableStateOf(false) }

    val tumKullanicilar = remember { mutableStateListOf<Map<String, Any>>() }
    val antrenorListesi = remember { mutableStateListOf<String>() }

    // Filtrelenmiş listeleri cache'le (her frame'de yeniden hesaplanmaz)
    val antrenorKullanicilar by remember {
        derivedStateOf { tumKullanicilar.filter { it["rol"] == Rol.ANTRENOR.name } }
    }
    val uyeKullanicilar by remember {
        derivedStateOf { tumKullanicilar.filter { it["rol"] == Rol.UYE.name } }
    }

    // Kullanıcıları ve Antrenörleri yükle (Memory leak önleme: DisposableEffect)
    DisposableEffect(Unit) {
        val registration = db.collection("Kullanicilar").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                // Batch güncelleme: önce topla, sonra tek seferde ata
                val yeniListe = snapshot.documents.mapNotNull { it.data }
                val yeniAntrenorler = yeniListe
                    .filter { it["rol"] == Rol.ANTRENOR.name }
                    .map { it["adSoyad"].toString() }

                tumKullanicilar.clear()
                tumKullanicilar.addAll(yeniListe)
                antrenorListesi.clear()
                antrenorListesi.addAll(yeniAntrenorler)
            }
        }
        onDispose { registration.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("CEO Yönetim Paneli", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaviKoyuRenk)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Kayıt Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaviAcikRenk)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Yeni Kullanıcı / Üye Kaydet", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = yeniKullaniciAdSoyad,
                    onValueChange = { yeniKullaniciAdSoyad = it },
                    label = { Text("Ad Soyad") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = secilenKayitRolu == Rol.ANTRENOR, onClick = { secilenKayitRolu = Rol.ANTRENOR })
                    Text("Antrenör")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = secilenKayitRolu == Rol.UYE, onClick = { secilenKayitRolu = Rol.UYE })
                    Text("Üye")
                }

                if (secilenKayitRolu == Rol.UYE) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = baslangicTarihi,
                            onValueChange = { baslangicTarihi = it },
                            label = { Text("Başlangıç") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bitisTarihi,
                            onValueChange = { bitisTarihi = it },
                            label = { Text("Bitiş") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ucret,
                        onValueChange = { ucret = it },
                        label = { Text("Ücret (TL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Antrenör Seçme Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { antrenorMenuAcik = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (secilenAntrenor == "Antrenör Seçilmedi") "Antrenör Ata" else "Antrenör: $secilenAntrenor")
                        }
                        DropdownMenu(
                            expanded = antrenorMenuAcik,
                            onDismissRequest = { antrenorMenuAcik = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Seçimi Kaldır") },
                                onClick = {
                                    secilenAntrenor = "Antrenör Seçilmedi"
                                    antrenorMenuAcik = false
                                }
                            )
                            antrenorListesi.forEach { antrenor ->
                                DropdownMenuItem(
                                    text = { Text(antrenor) },
                                    onClick = {
                                        secilenAntrenor = antrenor
                                        antrenorMenuAcik = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (yeniKullaniciAdSoyad.isNotEmpty()) {
                            yukleniyor = true
                            val formatliKullaniciAdi = yeniKullaniciAdSoyad.turkceKarakterSadelestir()
                            val kullaniciVeri = mutableMapOf(
                                "kullaniciAdi" to formatliKullaniciAdi,
                                "adSoyad" to yeniKullaniciAdSoyad,
                                "rol" to secilenKayitRolu.name,
                                "sifre" to "12345678"
                            )
                            
                            if (secilenKayitRolu == Rol.UYE) {
                                kullaniciVeri["baslangicTarihi"] = baslangicTarihi
                                kullaniciVeri["bitisTarihi"] = bitisTarihi
                                kullaniciVeri["ucret"] = ucret
                                kullaniciVeri["antrenor"] = secilenAntrenor
                            }

                            db.collection("Kullanicilar").document(formatliKullaniciAdi).set(kullaniciVeri)
                                .addOnSuccessListener {
                                    yukleniyor = false
                                    Toast.makeText(context, "Kayıt Başarılı!", Toast.LENGTH_SHORT).show()
                                    yeniKullaniciAdSoyad = ""; ucret = ""
                                    secilenAntrenor = "Antrenör Seçilmedi"
                                }
                                .addOnFailureListener {
                                    yukleniyor = false
                                    Toast.makeText(context, "Hata!", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (yukleniyor) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Kaydet")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Üye ve Personel Yönetimi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            // ANTRENÖRLER GRUBU
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaviKoyuRenk,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Antrenörler", color = Color.White, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                }
            }
            
            items(antrenorKullanicilar) { antrenor ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(antrenor["adSoyad"].toString(), modifier = Modifier.weight(1f), fontSize = 14.sp)
                    
                    // Silme Butonu
                    IconButton(onClick = {
                        val kadi = antrenor["kullaniciAdi"].toString()
                        db.collection("Kullanicilar").whereEqualTo("kullaniciAdi", kadi).get()
                            .addOnSuccessListener { snp ->
                                for (d in snp.documents) db.collection("Kullanicilar").document(d.id).delete()
                                Toast.makeText(context, "Antrenör Silindi", Toast.LENGTH_SHORT).show()
                            }
                    }) {
                        Icon(Icons.Default.Delete, "Sil", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }

            // ÜYELER GRUBU
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    color = MaviAnaRenk,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Üyeler", color = Color.White, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                }
            }

            items(uyeKullanicilar) { uye ->
                var localMenuAcik by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uye["adSoyad"].toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Ücret: ${uye["ucret"] ?: "-"} TL", fontSize = 12.sp, color = Color.Gray)
                            }
                            
                            // Antrenör Değiştirme Butonu
                            Box {
                                TextButton(onClick = { localMenuAcik = true }) {
                                    Text(
                                        text = uye["antrenor"]?.toString() ?: "Antrenör Seç",
                                        fontSize = 12.sp,
                                        color = MaviKoyuRenk,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                DropdownMenu(expanded = localMenuAcik, onDismissRequest = { localMenuAcik = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Seçimi Kaldır") },
                                        onClick = {
                                            db.collection("Kullanicilar").whereEqualTo("kullaniciAdi", uye["kullaniciAdi"]).get()
                                                .addOnSuccessListener { snp ->
                                                    for (d in snp.documents) db.collection("Kullanicilar").document(d.id).update("antrenor", "Antrenör Seçilmedi")
                                                }
                                            localMenuAcik = false
                                        }
                                    )
                                    antrenorListesi.forEach { antName ->
                                        DropdownMenuItem(
                                            text = { Text(antName) },
                                            onClick = {
                                                db.collection("Kullanicilar").whereEqualTo("kullaniciAdi", uye["kullaniciAdi"]).get()
                                                    .addOnSuccessListener { snp ->
                                                        for (d in snp.documents) db.collection("Kullanicilar").document(d.id).update("antrenor", antName)
                                                    }
                                                localMenuAcik = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = {
                                val kadi = uye["kullaniciAdi"].toString()
                                db.collection("Kullanicilar").whereEqualTo("kullaniciAdi", kadi).get()
                                    .addOnSuccessListener { snp ->
                                        for (d in snp.documents) db.collection("Kullanicilar").document(d.id).delete()
                                        Toast.makeText(context, "Üye Silindi", Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Icon(Icons.Default.Delete, "Sil", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                yukleniyor = true
                db.collection("Kullanicilar").get().addOnSuccessListener { result ->
                    val batch = db.batch()
                    for (document in result) batch.delete(document.reference)
                    batch.commit().addOnSuccessListener {
                        val antrenorler = listOf(
                            Pair("Göktuğ Alaf", "goktugalaf"),
                            Pair("Güray Aydın", "gurayaydin"),
                            Pair("Emre Baş", "emrebas")
                        )
                        var eklenenSayisi = 0
                        antrenorler.forEach { (ad, kadi) ->
                            val veri = hashMapOf(
                                "adSoyad" to ad,
                                "kullaniciAdi" to kadi,
                                "rol" to Rol.ANTRENOR.name,
                                "sifre" to "12345678"
                            )
                            db.collection("Kullanicilar").add(veri).addOnCompleteListener {
                                eklenenSayisi++
                                if (eklenenSayisi == antrenorler.size) {
                                    yukleniyor = false
                                    Toast.makeText(context, "Sistem Sıfırlandı!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Veritabanını Sıfırla")
        }

        Button(onClick = geriDon, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Çıkış Yap") }
    }
}

@Composable
fun AntrenorPaneli(kullaniciAdi: String, geriDon: () -> Unit) {
    var hocaAdSoyad by remember { mutableStateOf("Yükleniyor...") }
    val ogrencilerim = remember { mutableStateListOf<Map<String, Any>>() }
    val dersTalepleri = remember { mutableStateListOf<Map<String, Any>>() }
    var secilenSekme by remember { mutableIntStateOf(0) } 
    var secilenProgramOgrencisi by remember { mutableStateOf<Map<String, Any>?>(null) }
    var programMenuAcik by remember { mutableStateOf(false) }
    
    // Haftalık program state'leri (Gün -> (Kategori -> Hareketler Listesi))
    val haftalikProgram = remember { 
        mutableStateMapOf<String, Map<String, List<String>>>() 
    }

    // Memory leak önleme: Listener'ları DisposableEffect ile temizle
    val ogrenciRegistration = remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
    val talepRegistration = remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }

    LaunchedEffect(kullaniciAdi) {
        db.collection("Kullanicilar").whereEqualTo("kullaniciAdi", kullaniciAdi).get()
            .addOnSuccessListener { snp ->
                if (!snp.isEmpty) {
                    hocaAdSoyad = snp.documents[0].getString("adSoyad") ?: "İsimsiz Antrenör"
                    
                    // Antrenörün öğrencilerini getir
                    ogrenciRegistration.value = db.collection("Kullanicilar")
                        .whereEqualTo("rol", Rol.UYE.name)
                        .whereEqualTo("antrenor", hocaAdSoyad)
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null) {
                                val yeniOgrenciler = snapshot.documents.mapNotNull { it.data }
                                ogrencilerim.clear()
                                ogrencilerim.addAll(yeniOgrenciler)
                            }
                        }

                    // Antrenör talepleri
                    talepRegistration.value = db.collection("AntrenorTalepleri")
                        .whereEqualTo("istenenAntrenor", hocaAdSoyad)
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null) {
                                val yeniTalepler = snapshot.documents.mapNotNull { doc ->
                                    doc.data?.toMutableMap()?.also { it["id"] = doc.id }
                                }
                                dersTalepleri.clear()
                                dersTalepleri.addAll(yeniTalepler)
                            }
                        }
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            ogrenciRegistration.value?.remove()
            talepRegistration.value?.remove()
        }
    }

    LaunchedEffect(secilenProgramOgrencisi) {
        if (secilenProgramOgrencisi != null) {
            val ogrenciKadi = secilenProgramOgrencisi!!["kullaniciAdi"].toString()
            db.collection("Programlar").document(ogrenciKadi).get().addOnSuccessListener { doc ->
                GUNLER.forEach { haftalikProgram[it] = emptyMap() } // Reset
                if (doc.exists()) {
                    val veri = doc.data
                    veri?.forEach { (gun, gunVerisi) ->
                        if (gunVerisi is Map<*, *>) {
                            val kategoriMap = mutableMapOf<String, List<String>>()
                            gunVerisi.forEach { (kat, hareketler) ->
                                if (hareketler is List<*>) {
                                    kategoriMap[kat.toString()] = hareketler.map { it.toString() }
                                }
                            }
                            haftalikProgram[gun.toString()] = kategoriMap
                        }
                    }
                }
            }
        }
    }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Antrenör Çalışma Alanı", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaviAnaRenk)
        Text("Hoş Geldiniz, $hocaAdSoyad", fontSize = 16.sp, color = Color.Gray)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InfoBox("Öğrenci", ogrencilerim.size.toString())
            InfoBox("Talep", dersTalepleri.count { it["durum"] == "Bekliyor" }.toString())
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sekme Seçimi
        TabRow(selectedTabIndex = secilenSekme, containerColor = Color.Transparent, contentColor = MaviAnaRenk) {
            Tab(selected = secilenSekme == 0, onClick = { secilenSekme = 0 }) {
                Text("Öğrencilerim", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = secilenSekme == 1, onClick = { secilenSekme = 1 }) {
                BadgedBox(badge = {
                    val bekleyenSayisi = dersTalepleri.count { it["durum"] == "Bekliyor" }
                    if (bekleyenSayisi > 0) Badge { Text(bekleyenSayisi.toString()) }
                }) {
                    Text("Talepler", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = secilenSekme == 2, onClick = { secilenSekme = 2 }) {
                Text("Program Yaz", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (secilenSekme) {
            0 -> {
                // ÖĞRENCİ LİSTESİ
                if (ogrencilerim.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Henüz size atanmış bir öğrenci bulunmuyor.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(ogrencilerim) { ogrenci ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(ogrenci["adSoyad"].toString(), fontWeight = FontWeight.Bold)
                                        Text("Bitiş: ${ogrenci["bitisTarihi"] ?: "-"}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    TextButton(onClick = { 
                                        secilenProgramOgrencisi = ogrenci
                                        secilenSekme = 2 
                                    }) {
                                        Text("Program Yaz", color = MaviAnaRenk)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // DERS TALEPLERİ LİSTESİ
                Column(Modifier.weight(1f)) {
                    if (dersTalepleri.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    db.collection("AntrenorTalepleri")
                                        .whereEqualTo("istenenAntrenor", hocaAdSoyad)
                                        .get()
                                        .addOnSuccessListener { snp ->
                                            val batch = db.batch()
                                            for (doc in snp.documents) batch.delete(doc.reference)
                                            batch.commit()
                                        }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(Modifier.width(4.dp))
                                Text("Listeyi Temizle", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (dersTalepleri.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Bekleyen ders talebi bulunmuyor.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(dersTalepleri) { talep ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)) // Hafif sarımtırak talep rengi
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(talep["istekYapanUye"].toString(), fontWeight = FontWeight.Bold)
                                            Text(talep["durum"].toString(), color = MaviKoyuRenk, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (talep["durum"] == "Bekliyor") {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = {
                                                    db.collection("AntrenorTalepleri").document(talep["id"].toString()).update("durum", "Reddedildi")
                                                }) {
                                                    Text("Reddet", color = Color.Red)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(onClick = {
                                                    db.collection("AntrenorTalepleri").document(talep["id"].toString()).update("durum", "Onaylandı")
                                                }) {
                                                    Text("Onayla")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Haftalık ders programı
                Column(Modifier.weight(1f)) {
                    // Öğrenci Seçme Dropdown (Her zaman üstte dursun)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { programMenuAcik = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(secilenProgramOgrencisi?.get("adSoyad")?.toString() ?: "Program Yazılacak Öğrenciyi Seçin")
                        }
                        DropdownMenu(
                            expanded = programMenuAcik, 
                            onDismissRequest = { programMenuAcik = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            if (ogrencilerim.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Size atanmış öğrenci bulunmuyor") },
                                    onClick = { programMenuAcik = false }
                                )
                            }
                            ogrencilerim.forEach { ogrenci ->
                                DropdownMenuItem(
                                    text = { Text(ogrenci["adSoyad"].toString()) },
                                    onClick = {
                                        secilenProgramOgrencisi = ogrenci
                                        programMenuAcik = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (secilenProgramOgrencisi != null) {

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaviAcikRenk.copy(alpha = 0.3f))
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(MaviAnaRenk, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(secilenProgramOgrencisi!!["adSoyad"].toString().take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(secilenProgramOgrencisi!!["adSoyad"].toString(), fontWeight = FontWeight.Bold)
                                    Text("Haftalık Antrenman Planı", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        // GÜNLER LİSTESİ (DİKEY VE İŞLEVSEL)
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(GUNLER) { gun ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF0F0F0))
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(gun, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaviKoyuRenk)
                                            Spacer(Modifier.weight(1f))
                                            if ((haftalikProgram[gun]?.size ?: 0) > 0) {
                                                Text("${haftalikProgram[gun]?.size} Bölge Aktif", fontSize = 11.sp, color = MaviAnaRenk, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                                        // KATEGORİ CHİPLERİ (YATAY KAYDIRILABİLİR)
                                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            KATEGORILER.forEach { kat ->
                                                var menuAcik by remember { mutableStateOf(false) }
                                                val seciliHareketler = haftalikProgram[gun]?.get(kat) ?: emptyList()
                                                
                                                FilterChip(
                                                    selected = seciliHareketler.isNotEmpty(),
                                                    onClick = { menuAcik = true },
                                                    label = { Text(kat) },
                                                    leadingIcon = {
                                                        if (seciliHareketler.isNotEmpty()) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                                        else Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaviAcikRenk,
                                                        selectedLabelColor = MaviKoyuRenk
                                                    )
                                                )

                                                DropdownMenu(
                                                    expanded = menuAcik,
                                                    onDismissRequest = { menuAcik = false },
                                                    modifier = Modifier.width(220.dp)
                                                ) {
                                                    Text(kat, Modifier.padding(12.dp), fontWeight = FontWeight.Bold, color = MaviAnaRenk)
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Tümünü Temizle", color = Color.Red) },
                                                        onClick = {
                                                            val guncelKategoriler = haftalikProgram[gun]?.toMutableMap() ?: mutableMapOf()
                                                            guncelKategoriler.remove(kat)
                                                            haftalikProgram[gun] = guncelKategoriler
                                                            menuAcik = false
                                                        }
                                                    )
                                                    HorizontalDivider()
                                                    HAREKETLER_MAP[kat]?.forEach { hareket ->
                                                        val seciliMi = seciliHareketler.contains(hareket)
                                                        DropdownMenuItem(
                                                            text = { 
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(hareket, modifier = Modifier.weight(1f))
                                                                    if (seciliMi) Icon(Icons.Default.Check, null, tint = MaviAnaRenk)
                                                                }
                                                            },
                                                            onClick = {
                                                                val guncelKategoriler = haftalikProgram[gun]?.toMutableMap() ?: mutableMapOf()
                                                                val yeniListe = if (seciliMi) seciliHareketler - hareket else seciliHareketler + hareket
                                                                if (yeniListe.isEmpty()) guncelKategoriler.remove(kat)
                                                                else guncelKategoriler[kat] = yeniListe
                                                                haftalikProgram[gun] = guncelKategoriler
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // SEÇİLİ HAREKETLERİN ÖZETİ
                                        val tumSeciliHareketler = haftalikProgram[gun]?.values?.flatten() ?: emptyList()
                                        if (tumSeciliHareketler.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = tumSeciliHareketler.joinToString(" • "),
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val ogrenciKadi = secilenProgramOgrencisi!!["kullaniciAdi"].toString()
                                db.collection("Programlar").document(ogrenciKadi).set(haftalikProgram.toMap())
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Program Başarıyla Kaydedildi!", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(55.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("PROGRAMI KAYDET", fontWeight = FontWeight.ExtraBold)
                        }
                        
                        TextButton(
                            onClick = { secilenProgramOgrencisi = null },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Başka Öğrenci Seç", color = Color.Gray)
                        }
                    } else {
                        // Eğer öğrenci seçilmediyse bir liste göster
                        Text("Program yazmak için aşağıdaki listeden bir öğrenci seçin:", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(ogrencilerim) { ogrenci ->
                                Card(
                                    onClick = { secilenProgramOgrencisi = ogrenci },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1))
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaviAnaRenk, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(ogrenci["adSoyad"].toString(), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = geriDon,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Çıkış Yap")
        }
    }
}

@Composable
fun UyePaneli(kullaniciAdi: String, geriDon: () -> Unit) {
    val db = Firebase.firestore
    var uyeAdSoyad by remember { mutableStateOf("Yükleniyor...") }
    var mevcutAntrenor by remember { mutableStateOf("Henüz Atanmadı") }
    var ceoAtananAntrenor by remember { mutableStateOf("Henüz Atanmadı") }
    var mevcutAntrenorMail by remember { mutableStateOf("-") }
    var bekleyenIstekAntrenoru by remember { mutableStateOf<String?>(null) }
    val digerAntrenorler = remember { mutableStateListOf<String>() }
    
    // Üyelik Verileri (Dinamik)
    var uyelikBaslangic by remember { mutableStateOf("-") }
    var uyelikBitis by remember { mutableStateOf("-") }
    var kalanGun by remember { mutableIntStateOf(0) }
    var toplamGun by remember { mutableIntStateOf(1) }

    // Memory leak önleme: Talep listenerı
    val uyeTalepRegistration = remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }

    LaunchedEffect(kullaniciAdi) {
        // Üyenin kendi verilerini çek
        db.collection("Kullanicilar")
            .whereEqualTo("kullaniciAdi", kullaniciAdi)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val data = snapshot.documents[0]
                    uyeAdSoyad = data.getString("adSoyad") ?: "İsimsiz Üye"
                    val baslangicStr = data.getString("baslangicTarihi") ?: "01.01.2026"
                    val bitisStr = data.getString("bitisTarihi") ?: "01.01.2027"
                    ceoAtananAntrenor = data.getString("antrenor") ?: "Henüz Atanmadı"
                    mevcutAntrenor = ceoAtananAntrenor // Varsayılan olarak CEO'nun atadığı
                    
                    uyelikBaslangic = baslangicStr
                    uyelikBitis = bitisStr
                    
                    try {
                        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val bugun = Calendar.getInstance()
                        val baslangicDate = sdf.parse(baslangicStr)
                        val bitisDate = sdf.parse(bitisStr)
                        
                        if (baslangicDate != null && bitisDate != null) {
                            val toplamMilis = bitisDate.time - baslangicDate.time
                            val kalanMilis = bitisDate.time - bugun.timeInMillis
                            
                            val toplam = (toplamMilis / (1000 * 60 * 60 * 24)).toInt()
                            val kalan = (kalanMilis / (1000 * 60 * 60 * 24)).toInt()
                            
                            toplamGun = if (toplam > 0) toplam else 1
                            kalanGun = if (kalan > 0) kalan else 0
                        }
                    } catch (e: Exception) {
                        Log.e("TARIH_HATA", "Hesaplama hatası: ${e.message}")
                        toplamGun = 30
                        kalanGun = 0
                    }

                    // Onaylı talepleri takip et
                    uyeTalepRegistration.value = db.collection("AntrenorTalepleri")
                        .whereEqualTo("istekYapanUye", uyeAdSoyad)
                        .addSnapshotListener { talepSnapshot, _ ->
                            if (talepSnapshot != null) {
                                var bugunkuOnayliHoca: String? = null
                                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                val bugunStr = sdf.format(Calendar.getInstance().time)

                                for (doc in talepSnapshot.documents) {
                                    val durum = doc.getString("durum")
                                    val hoca = doc.getString("istenenAntrenor")
                                    val tarih = doc.getTimestamp("tarih")
                                    
                                    if (durum == "Onaylandı" && tarih != null) {
                                        val talepTarihiStr = sdf.format(tarih.toDate())
                                        // Eğer talep bugüne aitse geçici hoca olur
                                        if (talepTarihiStr == bugunStr) {
                                            bugunkuOnayliHoca = hoca
                                        }
                                    }
                                    
                                    // Bekleyen istek varsa kilitlemek için
                                    if (durum == "Bekliyor") {
                                        bekleyenIstekAntrenoru = hoca
                                    }
                                }
                                
                                // Mantık: Bugün onaylı bir talep varsa onu göster, yoksa CEO'nun atadığını göster
                                mevcutAntrenor = bugunkuOnayliHoca ?: ceoAtananAntrenor
                            }
                        }
                }
            }

        // Antrenörleri çek
        db.collection("Kullanicilar")
            .whereEqualTo("rol", Rol.ANTRENOR.name)
            .get()
            .addOnSuccessListener { documents ->
                digerAntrenorler.clear()
                for (doc in documents) {
                    digerAntrenorler.add(doc.getString("adSoyad") ?: "İsimsiz Antrenör")
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose { uyeTalepRegistration.value?.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Üye Paneli", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaviKoyuRenk)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaviAcikRenk)

        // Üyelik Bilgi Kartı (Yeni Eklenen Kısım)
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaviAnaRenk, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Üyelik Durumu", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))
                    Text("Aktif Plan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Başlangıç: $uyelikBaslangic", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Bitiş: $uyelikBitis", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { kalanGun.toFloat() / toplamGun },
                            modifier = Modifier.size(65.dp),
                            color = MaviAcikRenk,
                            strokeWidth = 6.dp,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Text("$kalanGun", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Kalan Gün", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaviAcikRenk)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mevcut Antrenörüm", fontWeight = FontWeight.Bold, color = MaviKoyuRenk)
                Spacer(modifier = Modifier.height(4.dp))
                Text(mevcutAntrenor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Text(mevcutAntrenorMail, fontSize = 14.sp, color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Salondaki Diğer Antrenörler", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Sadece bir antrenöre katılım isteği gönderebilirsiniz.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (digerAntrenorler.isEmpty()) {
                item {
                    Text("Kayıtlı antrenör bulunamadı.", modifier = Modifier.padding(8.dp), color = Color.Gray)
                }
            }

            items(digerAntrenorler) { antrenorAdi ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(antrenorAdi, fontSize = 16.sp, fontWeight = FontWeight.Medium)

                        if (bekleyenIstekAntrenoru == antrenorAdi) {
                            Text("İstek Bekliyor ⏳", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        } else if (bekleyenIstekAntrenoru != null) {
                            Text("Kilitli 🔒", color = Color.Gray)
                        } else {
                            Button(
                                onClick = {
                                    val talepVerisi = hashMapOf(
                                        "istekYapanUye" to uyeAdSoyad,
                                        "istenenAntrenor" to antrenorAdi,
                                        "durum" to "Bekliyor",
                                        "tarih" to com.google.firebase.Timestamp.now()
                                    )

                                    db.collection("AntrenorTalepleri")
                                        .add(talepVerisi)
                                        .addOnSuccessListener {
                                            bekleyenIstekAntrenoru = antrenorAdi
                                            Log.d("FIREBASE_TEST", "Başarılı: Firebase'e talep gitti!")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FIREBASE_TEST", "Hata oluştu: ${e.message}")
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaviAnaRenk)
                            ) {
                                Text("İstek At")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = geriDon,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Çıkış Yap")
        }
    }
}
// Alt kısımdaki gereksiz/kopya paneller temizlendi
@Composable
fun InfoBox(label: String, value: String) {
    Card(modifier = Modifier.width(100.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
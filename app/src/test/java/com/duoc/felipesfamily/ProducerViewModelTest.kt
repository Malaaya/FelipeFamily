package com.duoc.felipesfamily

import android.app.Application
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.Roles
import com.duoc.felipesfamily.data.ServiceEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.repository.ProducerRepository
import com.duoc.felipesfamily.viewmodel.ProducerViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProducerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProducerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkConstructor(ProducerRepository::class)

        coEvery { anyConstructed<ProducerRepository>().allServices() } returns listOf(
            ServiceEntity(1L, "Fumigación con dron", "zona, cultivo", 40)
        )
        coEvery { anyConstructed<ProducerRepository>().needsForProducer(any()) } returns emptyList()
        coEvery { anyConstructed<ProducerRepository>().unseenNotifications(any()) } returns emptyList()
        coEvery { anyConstructed<ProducerRepository>().allActiveProviders() } returns emptyList()

        val fakeApp = mockk<Application>(relaxed = true)
        viewModel = ProducerViewModel(fakeApp)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `submitNeed marca errores y no abre confirmacion si el formulario esta incompleto`() = runTest {
        viewModel.onFormChange(service = "", crop = "", surface = "0", zone = "", period = "")

        viewModel.submitNeed()

        assertFalse(viewModel.confirming.value)
        assertNotNull(viewModel.form.value.serviceError)
        assertNotNull(viewModel.form.value.cropError)
        assertNotNull(viewModel.form.value.surfaceError)
        assertNotNull(viewModel.form.value.zoneError)
        assertNotNull(viewModel.form.value.periodError)
    }

    @Test
    fun `submitNeed abre dialogo de confirmacion cuando todos los campos son validos`() = runTest {
        viewModel.onFormChange(
            service = "Fumigación con dron",
            crop = "Trigo",
            surface = "15",
            zone = "Zona Sur-2",
            period = "20/11 - 28/11"
        )

        viewModel.submitNeed()

        assertTrue(viewModel.confirming.value)
    }

    @Test
    fun `selectNeed consulta el resumen meteorologico en el repositorio`() = runTest {
        val producer = UserEntity(id = 1L, alias = "productor_demo01", passwordHash = "123", role = Roles.PRODUCER, zone = "Zona Sur-2")
        viewModel.setUser(producer)
        advanceUntilIdle()

        val need = NeedEntity(
            id = 5L,
            producerId = 1L,
            service = "Fumigación con dron",
            crop = "Trigo",
            surfaceHa = 12.0,
            zone = "Zona Sur-2",
            period = "20/11 - 25/11",
            product = "Líquido"
        )
        val expectedWeather = "Clima actual en Zona Sur-2: 18.0°C · Viento 10.0 km/h (Viento apto para dron)"
        coEvery { anyConstructed<ProducerRepository>().fetchZoneWeatherSummary("Zona Sur-2") } returns expectedWeather

        viewModel.selectNeed(need) {}
        advanceUntilIdle()

        assertEquals(expectedWeather, viewModel.weatherSummary.value)
        coVerify(exactly = 1) { anyConstructed<ProducerRepository>().fetchZoneWeatherSummary("Zona Sur-2") }
    }
}
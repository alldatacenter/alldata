/*
 * Copyright 2021 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { HttpClientTestingModule } from '@angular/common/http/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing'
import { FormsModule } from '@angular/forms'
import { MatAutocompleteModule } from '@angular/material/autocomplete'
import { By } from '@angular/platform-browser'
import { SplineTranslateTestingModule } from 'spline-utils/translate'

import { SplineSearchBoxComponent } from '../spline-search-box.component'


describe('SplineSearchComponent', () => {
    let component: SplineSearchBoxComponent
    let fixture: ComponentFixture<SplineSearchBoxComponent>

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                FormsModule,
                HttpClientTestingModule,
                // TranslateModule,
                SplineTranslateTestingModule,
                MatAutocompleteModule,
            ],
            declarations: [SplineSearchBoxComponent],
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
        })
            .compileComponents()
    }))

    beforeEach(() => {
        fixture = TestBed.createComponent(SplineSearchBoxComponent)
        component = fixture.componentInstance
        fixture.detectChanges()
    })

    function extractInputNativeElm(componentFixture: ComponentFixture<SplineSearchBoxComponent>): HTMLInputElement {
        return componentFixture.debugElement.query(By.css('.spline-search-box__input')).nativeElement
    }

    test('component should be created', () => {
        expect(component).toBeTruthy()
    })

    test('default search term settings', fakeAsync(() => {
        spyOn(component.search$, 'emit')
        const defaultValue = 'default search value'
        component.searchTerm = defaultValue
        fixture.detectChanges()
        tick(component.emitSearchEventDebounceTimeInUs)
        fixture.whenStable().then(() => {
            expect(component.inputValue).toEqual(defaultValue)
            const inputDomElm = extractInputNativeElm(fixture)
            expect(inputDomElm.value).toEqual(defaultValue)
            // do not emit event for default value initialization
            expect(component.search$['emit']).toHaveBeenCalledTimes(0)
        })
    }))

    test('value changed => emit value', fakeAsync(() => {

        spyOn(component.search$, 'emit')

        // set new value
        const newValue = 'new value'
        const inputDomElm = extractInputNativeElm(fixture)
        inputDomElm.value = newValue
        inputDomElm.dispatchEvent(new Event('keyup'))
        fixture.detectChanges()
        tick(component.emitSearchEventDebounceTimeInUs)

        fixture.whenStable().then(() => {
            expect(component.inputValue).toEqual(newValue)
            expect(component.search$['emit']).toHaveBeenCalledTimes(1)
            expect(component.search$['emit']).toHaveBeenCalledWith(newValue)
        })
    }))
})

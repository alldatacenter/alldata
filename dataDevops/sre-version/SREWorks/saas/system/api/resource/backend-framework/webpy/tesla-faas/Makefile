
publish:
	rm -rf ./dist/*
	python setup.py clean
	python setup.py bdist_wheel
	twine check ./dist/*
	twine upload -r pypiantfin ./dist/*

compile:
	rm -rf ./dist/*
	python setup.py clean
	python setup.py bdist_wheel
	twine check ./dist/*
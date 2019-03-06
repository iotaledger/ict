export const toDate = (timestamp, long) => {
	const date = new Date(timestamp)
		.toLocaleString('en-GB', { hour12: false })
		.replace(/\//g, '.')
		.replace(',', '');
	return long ? date.substr(0, date.length - 3) : date.substr(-8, 5);
};

export const moduleURI = (name, port) => {
	let url = `${window.location.protocol}//${window.location.hostname}`;
	return port ? `${url}:${port}/` : `${url}:${window.location.port}/modules/${name}/`;
};

export const downloadFile = (fileName, content) => {
	var element = document.createElement('a');
	element.setAttribute('href', 'data:text/plain;charset=utf-8,' + content);
	element.setAttribute('download', fileName);

	element.style.display = 'none';
	document.body.appendChild(element);

	element.click();

	document.body.removeChild(element);
};

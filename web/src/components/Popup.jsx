/* global window */
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';

import Card from './Card';
import Icon from './Icon';

class Popup extends PureComponent {
	static propTypes = {
		title: PropTypes.string,
		cta: PropTypes.string,
		loading: PropTypes.bool,
		type: PropTypes.oneOf(['warning', 'success']),
		children: PropTypes.node.isRequired,
		onClose: PropTypes.func,
		onConfirm: PropTypes.func
	};

	static defaultProps = {
		title: '',
		cta: 'Yes',
		loading: false,
		type: 'success',
		onClose: () => {},
		onConfirm: null
	};

	componentDidMount() {
		window.addEventListener('keydown', this.onKeyDown, false);
	}

	componentWillUnmount() {
		window.removeEventListener('keydown', this.onKeyDown, false);
	}

	onKeyDown = (e) => {
		const { onClose } = this.props;

		if (e.key === 'Escape' && onClose) {
			onClose();
		}
	};

	render() {
		const { children, title, onClose, onConfirm, type, cta, loading } = this.props;

		return (
			<div className={`popup${loading ? ' loading' : ''}`}>
				<div>
					<Card title={title}>
						{onClose && (
							<nav className="corner">
								<button onClick={onClose} type="button" className="x">
									<Icon size={14} icon="x" />
								</button>
							</nav>
						)}
						{children}
						{onConfirm && (
							<fieldset className="confirm">
								<button className={`button ${type}`} type="button" onClick={onConfirm}>
									{cta}
								</button>
								<button className="button secondary" type="button" onClick={onClose}>
									Cancel
								</button>
							</fieldset>
						)}
					</Card>
				</div>
			</div>
		);
	}
}

export default Popup;
